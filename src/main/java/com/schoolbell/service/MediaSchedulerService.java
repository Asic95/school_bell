package com.schoolbell.service;

import com.schoolbell.MainApp;
import com.schoolbell.model.BellEntry;
import com.schoolbell.model.MediaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(MediaSchedulerService.class);
    private final MainApp mainApp;
    private final AudioService audioService;
    private final List<MediaEvent> events = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Random random = new Random();

    private LocalTime lastCheckedMinute = null;
    private boolean isScheduledMediaPlaying = false;
    private final java.util.Set<String> playedEventsInCurrentBreak = new java.util.HashSet<>();
    private final java.util.Set<String> playedEventsToday = new java.util.HashSet<>();
    private java.time.LocalDate lastCheckedDate = java.time.LocalDate.now();
    private final long startupTimeMs = System.currentTimeMillis();
    
    private long lastAudioFinishedTimeMs = 0;
    private boolean wasAudioPlayingLastSecond = false;

    public MediaSchedulerService(MainApp mainApp) {
        this.mainApp = mainApp;
        this.audioService = mainApp.getAudioService();
        loadEvents();
        startChecking();
    }

    public void loadEvents() {
        events.clear();
        events.addAll(DatabaseManager.getAllMediaEvents());
        logger.info("Loaded {} media events", events.size());
    }

    private void startChecking() {
        scheduler.scheduleAtFixedRate(this::checkEvents, 0, 1, TimeUnit.SECONDS);
    }

    private void checkEvents() {
        // Block all scheduled media events if an emergency alert is active
        String alertType = mainApp.getSignalService().getCurrentAlertType();
        if ("AIR_RAID".equals(alertType) || "EMERGENCY".equals(alertType)) {
            // ONLY stop if it's break music. Don't touch the emergency alert sound!
            if (isScheduledMediaPlaying) {
                stopScheduledMedia();
            }
            return;
        }

        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1-7

        // Clear played events on new day
        if (!today.equals(lastCheckedDate)) {
            playedEventsToday.clear();
            lastCheckedDate = today;
        }

        // Track when the scheduled media finishes playing
        String currentTrack = audioService.getCurrentPlayingTrack();
        boolean isAudioPlayingNow = (currentTrack != null);
        if (wasAudioPlayingLastSecond && !isAudioPlayingNow) {
            lastAudioFinishedTimeMs = System.currentTimeMillis();
        }
        wasAudioPlayingLastSecond = isAudioPlayingNow;

        // Process dynamic triggers (checked every second)
        processDynamicEvents(now, dayOfWeek);

        // Check for specific time events (once per minute)
        if (lastCheckedMinute == null || !lastCheckedMinute.equals(now.withSecond(0).withNano(0))) {
            lastCheckedMinute = now.withSecond(0).withNano(0);
            processTimeEvents(now, dayOfWeek, today);
            processBreakEvents(now, dayOfWeek);
        }

        // Logic for stopping/fading scheduled media before bell
        handleScheduledMediaStop(now);
    }

    private void processTimeEvents(LocalTime now, int dayOfWeek, LocalDate today) {
        String nowStr = String.format("%02d:%02d", now.getHour(), now.getMinute());
        for (MediaEvent event : events) {
            if (!event.isActive()) continue;

            if ("TIME".equals(event.type())) {
                if (nowStr.equals(event.time()) && event.daysOfWeek().contains(String.valueOf(dayOfWeek))) {
                    playEvent(event);
                }
            } else if ("ONCE".equals(event.type())) {
                if (nowStr.equals(event.time()) && today.toString().equals(event.date())) {
                    playEvent(event);
                }
            }
        }
    }

    private void processBreakEvents(LocalTime now, int dayOfWeek) {
        List<BellEntry> schedule = mainApp.getSchedule();
        if (schedule == null || schedule.isEmpty()) return;

        boolean currentlyInAnyBreak = false;

        for (int i = 0; i < schedule.size() - 1; i++) {
            BellEntry current = schedule.get(i);
            BellEntry next = schedule.get(i + 1);

            if (current.type().contains("кінець") && next.type().contains("початок")) {
                LocalTime start = current.time();
                LocalTime end = next.time();

                if (now.isAfter(start) && now.isBefore(end)) {
                    currentlyInAnyBreak = true;
                    String breakId = start.toString() + "-" + end.toString();
                    
                    for (MediaEvent event : events) {
                        if (!event.isActive() || !"BREAKS".equals(event.type())) continue;
                        
                        // Check if today is an active day for this event
                        if (event.daysOfWeek() != null && !event.daysOfWeek().contains(String.valueOf(dayOfWeek))) continue;

                        String trackingKey = event.id() + "@" + breakId;
                        if (playedEventsInCurrentBreak.contains(trackingKey)) continue;

                        LocalTime triggerTime = calculateTriggerTime(event, start, end);
                        if (triggerTime != null) {
                            // Trigger if we are at or slightly after the trigger time within the same break
                            if (!now.isBefore(triggerTime) && now.isBefore(end)) {
                                playEvent(event);
                                playedEventsInCurrentBreak.add(trackingKey);
                            }
                        }
                    }
                }
            }
        }

        if (!currentlyInAnyBreak) {
            playedEventsInCurrentBreak.clear();
        }
    }

    private LocalTime calculateTriggerTime(MediaEvent event, LocalTime breakStart, LocalTime breakEnd) {
        String anchor = event.breakAnchor() != null ? event.breakAnchor() : "START";
        int offset = event.breakOffset();
        long durationSec = java.time.Duration.between(breakStart, breakEnd).getSeconds();

        return switch (anchor) {
            case "START" -> breakStart;
            case "END" -> breakEnd.minusMinutes(offset > 0 ? offset : 2); // default 2 min before end if not set
            case "MIDDLE" -> breakStart.plusSeconds(durationSec / 2);
            case "OFFSET" -> breakStart.plusMinutes(offset);
            default -> breakStart;
        };
    }

    private void handleScheduledMediaStop(LocalTime now) {
        if (!isScheduledMediaPlaying) return;

        List<BellEntry> schedule = mainApp.getSchedule();
        ConfigService config = mainApp.getConfigService();
        if (schedule == null || schedule.isEmpty()) return;

        int earlyMin = config.getEarlyBellMinutes();
        int earlySec = config.getEarlyBellSeconds();
        int totalEarlyOffsetSeconds = (earlyMin * 60) + earlySec;

        boolean shouldStop = false;
        long minSecondsToBell = -1;

        for (BellEntry entry : schedule) {
            LocalTime bellTime = entry.time();
            if (bellTime.isAfter(now)) {
                long diff = java.time.Duration.between(now, bellTime).getSeconds();
                if (minSecondsToBell == -1 || diff < minSecondsToBell) {
                    minSecondsToBell = diff;
                }
            }
        }

        if (minSecondsToBell != -1) {
            long secondsToStop;
            if (totalEarlyOffsetSeconds > 0) {
                secondsToStop = minSecondsToBell - totalEarlyOffsetSeconds - 3;
            } else {
                secondsToStop = minSecondsToBell - 10; // Standard 10s fade for normal bells
            }

            if (secondsToStop <= 0) {
                shouldStop = true;
            }
        }

        if (shouldStop) {
            stopScheduledMedia();
        }
    }

    private void stopScheduledMedia() {
        audioService.stopAll();
        isScheduledMediaPlaying = false;
    }

    private void playEvent(MediaEvent event) {
        isScheduledMediaPlaying = true;
        new Thread(() -> {
            // Wait for bell to finish if it's currently ringing
            boolean hadBell = false;
            while (mainApp.getSignalService().isActionInProgress()) {
                hadBell = true;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }

            // If we waited for a bell, add a 3-second grace period
            if (hadBell) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    return;
                }
            }

            // If another media event is currently playing, fade it out before the silence gap.
            if (audioService.getCurrentPlayingTrack() != null) {
                audioService.stopAll();
                try {
                    Thread.sleep(4000); // 2.5 seconds fade-out + 1.5 seconds pause
                } catch (InterruptedException e) {
                    return;
                }
            } else {
                // If it finished naturally, ensure a minimum of 1.5 seconds of silence since last track
                long elapsed = System.currentTimeMillis() - lastAudioFinishedTimeMs;
                long requiredPauseMs = 1500;
                if (elapsed < requiredPauseMs) {
                    try {
                        Thread.sleep(requiredPauseMs - elapsed);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }

            String path = event.path();
            if (event.isFolder()) {
                File folder = new File(path);
                if (folder.exists() && folder.isDirectory()) {
                    File[] files = folder.listFiles((dir, name) -> {
                        String n = name.toLowerCase();
                        return n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".m4a") || n.endsWith(".aac");
                    });
                    if (files != null && files.length > 0) {
                        List<File> playlist = new ArrayList<>(List.of(files));
                        // Shuffle for variety
                        java.util.Collections.shuffle(playlist);
                        logger.info("Starting media event (Folder): {} -> {} tracks", event.name(), playlist.size());
                        audioService.playPlaylist(playlist, true); // Loop playlist for folders
                        return;
                    }
                }
            }
            
            logger.info("Starting media event (Source): {} -> {}", event.name(), path);
            audioService.playAudioFile(path, event.name());
        }, "MediaEvent-Play-Thread").start();
    }

    private void processDynamicEvents(LocalTime now, int dayOfWeek) {
        long elapsedMs = System.currentTimeMillis() - startupTimeMs;

        for (MediaEvent event : events) {
            if (!event.isActive()) continue;
            if (playedEventsToday.contains(event.id().toString())) continue;

            if ("RANGE".equals(event.type())) {
                if (event.daysOfWeek() != null && !event.daysOfWeek().contains(String.valueOf(dayOfWeek))) continue;
                if (elapsedMs < event.breakOffset() * 1000L) continue;

                String timeRange = event.time();
                if (timeRange == null || !timeRange.contains("-")) continue;

                try {
                    String[] parts = timeRange.split("-");
                    LocalTime start = LocalTime.parse(parts[0].trim());
                    LocalTime end = LocalTime.parse(parts[1].trim());

                    if (!now.isBefore(start) && now.isBefore(end)) {
                        playedEventsToday.add(event.id().toString());
                        playEvent(event);
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse time range for event: " + event.name(), e);
                }
            } else if ("FIRST_LESSON".equals(event.type())) {
                if (event.daysOfWeek() != null && !event.daysOfWeek().contains(String.valueOf(dayOfWeek))) continue;

                List<BellEntry> schedule = mainApp.getSchedule();
                if (schedule == null || schedule.isEmpty()) continue;

                LocalTime firstLessonTime = null;
                for (BellEntry entry : schedule) {
                    if (entry.type().toLowerCase().contains("початок")) {
                        firstLessonTime = entry.time();
                        break;
                    }
                }

                if (firstLessonTime == null) continue;

                LocalTime targetTime = firstLessonTime.minusMinutes(event.breakOffset());
                if (!now.isBefore(targetTime) && now.isBefore(firstLessonTime)) {
                    long minutesLate = java.time.Duration.between(targetTime, now).toMinutes();
                    if (minutesLate <= event.durationMinutes()) {
                        playedEventsToday.add(event.id().toString());
                        playEvent(event);
                    }
                }
            }
        }
    }

    public List<MediaEvent> getEvents() { return events; }

    public void addEvent(MediaEvent event) {
        DatabaseManager.saveMediaEvent(event);
        loadEvents();
    }

    public void updateEvent(MediaEvent event) {
        DatabaseManager.saveMediaEvent(event);
        loadEvents();
    }

    public void deleteEvent(int id) {
        DatabaseManager.deleteMediaEvent(id);
        loadEvents();
    }

    public MediaEvent getNextEvent() {
        LocalTime now = LocalTime.now();
        java.time.LocalDate today = java.time.LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        MediaEvent next = null;
        LocalTime minTime = LocalTime.MAX;

        List<BellEntry> schedule = mainApp.getSchedule();

        for (MediaEvent e : events) {
            if (!e.isActive()) continue;
            
            // Check if it's already played today
            if (playedEventsToday.contains(e.id().toString())) continue;
            
            // Day/Date check
            if ("TIME".equals(e.type()) || "BREAKS".equals(e.type()) || "RANGE".equals(e.type()) || "FIRST_LESSON".equals(e.type())) {
                if (e.daysOfWeek() == null || !e.daysOfWeek().contains(String.valueOf(dayOfWeek))) continue;
            } else if ("ONCE".equals(e.type())) {
                if (!today.toString().equals(e.date())) continue;
            }

            try {
                if ("BREAKS".equals(e.type())) {
                    // Calculate next break trigger
                    if (schedule != null && !schedule.isEmpty()) {
                        for (int i = 0; i < schedule.size() - 1; i++) {
                            BellEntry curr = schedule.get(i);
                            BellEntry nxt = schedule.get(i + 1);
                            if (curr.type().contains("кінець") && nxt.type().contains("початок")) {
                                LocalTime trigger = calculateTriggerTime(e, curr.time(), nxt.time());
                                if (trigger.isAfter(now) && trigger.isBefore(minTime)) {
                                    minTime = trigger;
                                    next = new MediaEvent(e.id(), e.name(), e.path(), e.type(), trigger.toString(), e.daysOfWeek(), e.date(), e.isActive(), e.isFolder(), e.durationMinutes(), e.breakAnchor(), e.breakOffset());
                                }
                            }
                        }
                    }
                } else if ("RANGE".equals(e.type())) {
                    String timeRange = e.time();
                    if (timeRange != null && timeRange.contains("-")) {
                        String[] parts = timeRange.split("-");
                        LocalTime start = LocalTime.parse(parts[0].trim());
                        LocalTime end = LocalTime.parse(parts[1].trim());
                        if (now.isBefore(end)) {
                            // Calculate effective start considering program startup delay
                            long startupTriggerMs = startupTimeMs + e.breakOffset() * 1000L;
                            LocalTime startupTriggerTime = LocalTime.ofInstant(java.time.Instant.ofEpochMilli(startupTriggerMs), java.time.ZoneId.systemDefault());
                            LocalTime effectiveStart = start;
                            if (startupTriggerTime.isAfter(effectiveStart)) {
                                effectiveStart = startupTriggerTime;
                            }

                            LocalTime trigger = now.isBefore(effectiveStart) ? effectiveStart : now.withNano(0);
                            if (trigger.isBefore(minTime)) {
                                minTime = trigger;
                                String triggerStr = trigger.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                                next = new MediaEvent(e.id(), e.name(), e.path(), e.type(), triggerStr, e.daysOfWeek(), e.date(), e.isActive(), e.isFolder(), e.durationMinutes(), e.breakAnchor(), e.breakOffset());
                            }
                        }
                    }
                } else if ("FIRST_LESSON".equals(e.type())) {
                    if (schedule != null && !schedule.isEmpty()) {
                        LocalTime firstLessonTime = null;
                        for (BellEntry entry : schedule) {
                            if (entry.type().toLowerCase().contains("початок")) {
                                firstLessonTime = entry.time();
                                break;
                            }
                        }
                        if (firstLessonTime != null) {
                            LocalTime targetTime = firstLessonTime.minusMinutes(e.breakOffset());
                            if (now.isBefore(firstLessonTime)) {
                                LocalTime trigger = now.isBefore(targetTime) ? targetTime : now.withNano(0);
                                if (trigger.isBefore(minTime)) {
                                    minTime = trigger;
                                    String triggerStr = trigger.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                                    next = new MediaEvent(e.id(), e.name(), e.path(), e.type(), triggerStr, e.daysOfWeek(), e.date(), e.isActive(), e.isFolder(), e.durationMinutes(), e.breakAnchor(), e.breakOffset());
                                }
                            }
                        }
                    }
                } else {
                    LocalTime eventTime = LocalTime.parse(e.time());
                    if (eventTime.isAfter(now) && eventTime.isBefore(minTime)) {
                        minTime = eventTime;
                        next = e;
                    }
                }
            } catch (Exception ex) {}
        }
        return next;
    }

    public void stop() {
        scheduler.shutdown();
    }
}
