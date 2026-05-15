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
    private boolean isPlayingOnBreak = false;
    private final java.util.Set<String> playedEventsInCurrentBreak = new java.util.HashSet<>();

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
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1-7

        // Check for specific time events (once per minute)
        if (lastCheckedMinute == null || !lastCheckedMinute.equals(now.withSecond(0).withNano(0))) {
            lastCheckedMinute = now.withSecond(0).withNano(0);
            processTimeEvents(now, dayOfWeek, today);
            processBreakEvents(now, dayOfWeek);
        }

        // Logic for stopping/fading break music before bell
        handleBreakMusicStop(now);
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

            if ("OUT".equals(current.type()) && "IN".equals(next.type())) {
                LocalTime start = current.time();
                LocalTime end = next.time();

                if (now.isAfter(start) && now.isBefore(end)) {
                    currentlyInAnyBreak = true;
                    String breakId = start.toString() + "-" + end.toString();
                    
                    for (MediaEvent event : events) {
                        if (!event.isActive() || !"BREAKS".equals(event.type())) continue;
                        
                        String trackingKey = event.id() + "@" + breakId;
                        if (playedEventsInCurrentBreak.contains(trackingKey)) continue;

                        LocalTime triggerTime = calculateTriggerTime(event, start, end);
                        if (triggerTime != null && now.getHour() == triggerTime.getHour() && now.getMinute() == triggerTime.getMinute()) {
                            playEvent(event);
                            playedEventsInCurrentBreak.add(trackingKey);
                            if (event.breakAnchor().equals("START") || event.breakAnchor().equals("MIDDLE") || (event.breakAnchor().equals("OFFSET") && event.breakOffset() >= 0)) {
                                isPlayingOnBreak = true;
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

    private void handleBreakMusicStop(LocalTime now) {
        if (!isPlayingOnBreak) return;

        List<BellEntry> schedule = mainApp.getSchedule();
        if (schedule == null) return;

        boolean shouldStop = true;
        for (int i = 0; i < schedule.size() - 1; i++) {
            BellEntry current = schedule.get(i);
            BellEntry next = schedule.get(i + 1);

            if ("OUT".equals(current.type()) && "IN".equals(next.type())) {
                if (now.isAfter(current.time()) && now.isBefore(next.time())) {
                    long secondsToNext = java.time.Duration.between(now, next.time()).getSeconds();
                    if (secondsToNext > 10) {
                        shouldStop = false;
                    }
                    break;
                }
            }
        }

        if (shouldStop) {
            stopBreakMusic();
        }
    }

    private void stopBreakMusic() {
        audioService.stopAll();
        isPlayingOnBreak = false;
    }

    private void playEvent(MediaEvent event) {
        String path = event.path();
        if (event.isFolder()) {
            File folder = new File(path);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".wav"));
                if (files != null && files.length > 0) {
                    path = files[random.nextInt(files.length)].getAbsolutePath();
                }
            }
        }
        
        logger.info("Starting media event: {} -> {}", event.name(), path);
        audioService.playAudioFile(path);
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

    public void stop() {
        scheduler.shutdown();
    }
}
