package com.schoolbell.service;

import com.schoolbell.hardware.RelayController;
import com.schoolbell.model.BellEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SignalService {
    private static final Logger logger = LoggerFactory.getLogger(SignalService.class);

    private final RelayController relayController;
    private final AudioService audioService;
    private final ConfigService configService;

    private boolean isActionInProgress = false;
    private volatile boolean isAirRaidActive = false;
    private volatile boolean isEmergencyActive = false;
    private String currentAlertType = "NONE"; // NONE, AIR_RAID, EMERGENCY
    private BiConsumer<String, String> logConsumer;
    private LocalTime lastEarlyBellMinute = null;

    // Playlist state for folder-based audio
    private java.util.List<java.io.File> playlist = new java.util.ArrayList<>();
    private int currentPlaylistIndex = 0;
    private String lastPlaylistPath = "";

    public SignalService(RelayController relayController, AudioService audioService, ConfigService configService) {
        this.relayController = relayController;
        this.audioService = audioService;
        this.configService = configService;
    }

    /**
     * Resolves a configuration path (file or folder) to a specific audio file path.
     * If it's a folder, it shuffles the contents and maintains a playlist index.
     */
    private String resolveAudioPath(String configPath) {
        if (configPath == null || configPath.isBlank()) return null;
        java.io.File target = new java.io.File(configPath);
        if (!target.exists()) return configPath;

        if (target.isDirectory()) {
            // Re-initialize playlist if the path changed or playlist is empty
            if (!configPath.equals(lastPlaylistPath) || playlist.isEmpty()) {
                lastPlaylistPath = configPath;
                playlist.clear();
                java.io.File[] files = target.listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a");
                });
                
                if (files != null && files.length > 0) {
                    playlist.addAll(java.util.Arrays.asList(files));
                    java.util.Collections.shuffle(playlist);
                    currentPlaylistIndex = 0;
                }
            }

            if (playlist.isEmpty()) return null;

            // Get current file and increment index
            java.io.File nextFile = playlist.get(currentPlaylistIndex);
            currentPlaylistIndex = (currentPlaylistIndex + 1) % playlist.size();
            
            // If we've looped back to the start, re-shuffle for variety
            if (currentPlaylistIndex == 0) {
                java.util.Collections.shuffle(playlist);
            }
            
            return nextFile.getAbsolutePath();
        }

        return configPath;
    }

    public void setLogConsumer(BiConsumer<String, String> logConsumer) {
        this.logConsumer = logConsumer;
    }

    private void addLog(String message, String level) {
        logger.info("[{}] {}", level, message);
        if (logConsumer != null) {
            logConsumer.accept(message, level);
        }
    }

    public boolean isActionInProgress() {
        return isActionInProgress;
    }

    public String getCurrentAlertType() {
        return currentAlertType;
    }

    public boolean isAirRaidActive() {
        return isAirRaidActive || "AIR_RAID".equals(currentAlertType);
    }

    public boolean isEmergencyActive() {
        return isEmergencyActive || "EMERGENCY".equals(currentAlertType);
    }

    public void runAirRaidSignal() {
        if (isActionInProgress) return;
        isActionInProgress = true;
        isAirRaidActive = true;
        currentAlertType = "AIR_RAID";
        audioService.stopImmediate();
        addLog("ЗАПУСК СИГНАЛУ: ПОВІТРЯНА ТРИВОГА", "WARNING");
        new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    relayController.turnOn();
                    Thread.sleep(configService.getAirRaidRingDuration() * 1000L);
                    relayController.turnOff();
                    if (i < 3) Thread.sleep(configService.getAirRaidPauseDuration() * 1000L);
                }
                if (configService.isAudioAirRaidEnabled()) {
                    Thread.sleep(1500);
                    audioService.playAudioFile(resolveAudioPath(configService.getAudioAirRaidPath()));
                }
                addLog("Сигнал тривоги завершено. Режим ТРИВОГИ активовано.", "SUCCESS");
            } catch (InterruptedException e) {
                relayController.turnOff();
                currentAlertType = "NONE";
                isAirRaidActive = false;
            } finally {
                isActionInProgress = false;
            }
        }).start();
    }

    public void runAirRaidClearSignal() {
        if (isActionInProgress) return;
        isActionInProgress = true;
        addLog("ЗАПУСК СИГНАЛУ: ВІДБІЙ ПОВІТРЯНОЇ ТРИВОГИ", "SUCCESS");
        new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    relayController.turnOn();
                    Thread.sleep(configService.getAirRaidRingDuration() * 1000L);
                    relayController.turnOff();
                    if (i < 3) Thread.sleep(configService.getAirRaidPauseDuration() * 1000L);
                }
                
                if (configService.isAudioAirRaidEnabled()) {
                    String clearPath = configService.getAudioAirRaidClearPath();
                    if (clearPath != null && !clearPath.isBlank()) {
                        Thread.sleep(1500);
                        audioService.playAudioFile(resolveAudioPath(clearPath));
                    }
                }
                
                isAirRaidActive = false;
                currentAlertType = "NONE";
                addLog("Відбій тривоги завершено.", "SUCCESS");
            } catch (InterruptedException e) {
                relayController.turnOff();
                isAirRaidActive = false;
                currentAlertType = "NONE";
            } finally {
                isActionInProgress = false;
            }
        }).start();
    }

    public void runEmergencySignal() {
        if (isActionInProgress) return;
        isActionInProgress = true;
        isEmergencyActive = true;
        currentAlertType = "EMERGENCY";
        audioService.stopImmediate();
        addLog("ЗАПУСК СИГНАЛУ: НАДЗВИЧАЙНА СИТУАЦІЯ", "ERROR");
        new Thread(() -> {
            try {
                relayController.turnOn();
                Thread.sleep(configService.getEmergencyDuration() * 1000L);
                relayController.turnOff();
                if (configService.isAudioEmergencyEnabled()) {
                    Thread.sleep(1500);
                    audioService.playAudioFile(resolveAudioPath(configService.getAudioEmergencyPath()));
                }
                addLog("Сигнал НС завершено. Режим НАДЗВИЧАЙНОЇ СИТУАЦІЇ активовано.", "SUCCESS");
            } catch (InterruptedException e) {
                relayController.turnOff();
                currentAlertType = "NONE";
                isEmergencyActive = false;
            } finally {
                isActionInProgress = false;
            }
        }).start();
    }

    public void runEmergencyClearSignal() {
        if (isActionInProgress) return;
        isActionInProgress = true;
        addLog("ЗАПУСК СИГНАЛУ: СКАСУВАННЯ ЕКСТРЕНОЇ СИТУАЦІЇ", "SUCCESS");
        new Thread(() -> {
            try {
                // Дзеркально до сигналу НС: використовуємо налаштовану тривалість
                relayController.turnOn();
                Thread.sleep(configService.getEmergencyDuration() * 1000L);
                relayController.turnOff();
                isEmergencyActive = false;
                currentAlertType = "NONE";
                addLog("Екстрену ситуацію скасовано.", "SUCCESS");
            } catch (InterruptedException e) {
                relayController.turnOff();
                isEmergencyActive = false;
                currentAlertType = "NONE";
            } finally {
                isActionInProgress = false;
            }
        }).start();
    }

    /**
     * Plays the automation error sound (e.g. when API fails).
     * This sound plays even if other actions are in progress, 
     * but only if a path is configured.
     */
    public void playAutomationError() {
        String errorPath = configService.getAudioAirRaidErrorPath();
        if (errorPath != null && !errorPath.isBlank()) {
            String resolved = resolveAudioPath(errorPath);
            if (resolved != null) {
                audioService.playAudioFile(resolved);
            }
        }
    }

    public void testRelay() {
        if (isActionInProgress) return;
        new Thread(() -> {
            isActionInProgress = true;
            try {
                addLog("Тестування реле (імпульс 1 сек)...", "INFO");
                relayController.turnOn();
                Thread.sleep(1000);
                relayController.turnOff();
                addLog("Тест реле завершено.", "SUCCESS");
            } catch (InterruptedException e) {
                relayController.turnOff();
            } finally {
                isActionInProgress = false;
            }
        }).start();
    }

    public void setTemporaryAlertType(String type, int durationMs) {
        new Thread(() -> {
            currentAlertType = type;
            try {
                Thread.sleep(durationMs);
            } catch (InterruptedException ignored) {
            } finally {
                if (type.equals(currentAlertType)) {
                    currentAlertType = isAirRaidActive ? "AIR_RAID" : (isEmergencyActive ? "EMERGENCY" : "NONE");
                }
            }
        }).start();
    }

    public void triggerSilenceMinute() {
        if (!configService.isAudioSilenceEnabled()) return;

        new Thread(() -> {
            try {
                // If physical bell relay action is currently executing, wait briefly for it to complete
                int waitCount = 0;
                while (isActionInProgress && waitCount < 30) {
                    Thread.sleep(1000);
                    waitCount++;
                }
            } catch (InterruptedException ignored) {
            }

            boolean airRaid = isAirRaidActive();
            String logMsg = airRaid
                    ? "ВШАНУВАННЯ ПАМ'ЯТІ: Хвилина мовчання (під час повітряної тривоги)"
                    : "ВШАНУВАННЯ ПАМ'ЯТІ: Хвилина мовчання";
            addLog(logMsg, "INFO");

            String audioPath = resolveAudioPath(configService.getAudioSilencePath());
            if (audioPath != null && !audioPath.isBlank()) {
                audioService.playAudioFile(audioPath);
            }

            // In peacetime, activate the SILENCE visual banner for 65 seconds
            // During air raid, keep AIR_RAID banner active for safety
            if (!airRaid) {
                currentAlertType = "SILENCE";
                try {
                    Thread.sleep(65000); // 65 seconds
                } catch (InterruptedException ignored) {
                } finally {
                    if ("SILENCE".equals(currentAlertType)) {
                        currentAlertType = isAirRaidActive ? "AIR_RAID" : (isEmergencyActive ? "EMERGENCY" : "NONE");
                    }
                }
            }
        }, "Silence-Minute-Thread").start();
    }

    public void checkAndTriggerBell(LocalTime now, List<BellEntry> schedule) {
        // 1. Хвилина мовчання (09:00:00)
        // Працює о 09:00:00 навіть під час активної повітряної тривоги
        if (now.getHour() == 9 && now.getMinute() == 0 && now.getSecond() == 0) {
            if (configService.isAudioSilenceEnabled() && !isEmergencyActive() && !"EMERGENCY".equals(currentAlertType)) {
                triggerSilenceMinute();
            }
        }

        // 2. Блокуємо всі планові дзвінки за розкладом (ранні сповіщення та звичайні уроки),
        // якщо активовано режим повітряної тривоги або екстреної ситуації
        if (isAirRaidActive() || "AIR_RAID".equals(currentAlertType) || isEmergencyActive() || "EMERGENCY".equals(currentAlertType)) {
            return;
        }

        // --- Early Notification Bell Logic ---
        int earlyMin = configService.getEarlyBellMinutes();
        int earlySec = configService.getEarlyBellSeconds();
        if ((earlyMin > 0 || earlySec > 0) && !isActionInProgress) {
            LocalTime minuteOnly = now.truncatedTo(ChronoUnit.MINUTES);
            // We only trigger if we haven't already triggered an early bell for this specific minute
            if (lastEarlyBellMinute == null || !lastEarlyBellMinute.equals(minuteOnly)) {
                for (BellEntry entry : schedule) {
                    // Only for lesson starts
                    if (entry.type().contains("початок")) {
                        LocalTime triggerTime = entry.time().minusMinutes(earlyMin).minusSeconds(earlySec);
                        
                        // Check if now matches the triggerTime (with 1s tolerance)
                        if (now.getHour() == triggerTime.getHour() && 
                            now.getMinute() == triggerTime.getMinute() && 
                            now.getSecond() == triggerTime.getSecond()) {
                            
                            lastEarlyBellMinute = minuteOnly;
                            new Thread(() -> {
                                isActionInProgress = true;
                                try {
                                    addLog("🔔 ЗАВЧАСНЕ СПОВІЩЕННЯ: " + entry.type(), "INFO");
                                    relayController.turnOn();
                                    Thread.sleep(configService.getRegularBellDuration() * 1000L);
                                    relayController.turnOff();
                                } catch (InterruptedException e) {
                                    relayController.turnOff();
                                } finally {
                                    isActionInProgress = false;
                                }
                            }).start();
                            break; // Only one early bell at a time
                        }
                    }
                }
            }
        }
        
        if (now.getSecond() == 0 && !isActionInProgress) {
            LocalTime minuteOnly = now.truncatedTo(ChronoUnit.MINUTES);
            schedule.stream()
                    .filter(entry -> entry.time().equals(minuteOnly))
                    .findFirst()
                    .ifPresent(entry -> {
                        new Thread(() -> {
                            isActionInProgress = true;
                            try {
                                addLog("🔔 Автодзвінок: " + entry.type(), "SUCCESS");
                                relayController.turnOn();
                                Thread.sleep(configService.getRegularBellDuration() * 1000L);
                                relayController.turnOff();
                            } catch (InterruptedException e) {
                                relayController.turnOff();
                            } finally {
                                isActionInProgress = false;
                            }
                        }).start();
                    });
        }
    }
}
