package com.schoolbell.service;

import com.schoolbell.hardware.RelayController;
import com.schoolbell.model.BellEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

public class SignalService {
    private static final Logger logger = LoggerFactory.getLogger(SignalService.class);

    private final RelayController relayController;
    private final AudioService audioService;
    private final ConfigService configService;

    private boolean isActionInProgress = false;
    private String currentAlertType = "NONE"; // NONE, AIR_RAID, EMERGENCY
    private Consumer<String> logConsumer;

    public SignalService(RelayController relayController, AudioService audioService, ConfigService configService) {
        this.relayController = relayController;
        this.audioService = audioService;
        this.configService = configService;
    }

    public void setLogConsumer(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
    }

    private void addLog(String message, String level) {
        String fullMsg = "[" + level + "] " + message;
        logger.info(fullMsg);
        if (logConsumer != null) {
            logConsumer.accept(fullMsg);
        }
    }

    public boolean isActionInProgress() {
        return isActionInProgress;
    }

    public String getCurrentAlertType() {
        return currentAlertType;
    }

    public void runAirRaidSignal() {
        if (isActionInProgress) return;
        new Thread(() -> {
            isActionInProgress = true;
            currentAlertType = "AIR_RAID";
            addLog("ЗАПУСК СИГНАЛУ: ПОВІТРЯНА ТРИВОГА", "WARNING");
            try {
                for (int i = 1; i <= 3; i++) {
                    relayController.turnOn();
                    Thread.sleep(configService.getAirRaidRingDuration() * 1000L);
                    relayController.turnOff();
                    if (i < 3) Thread.sleep(configService.getAirRaidPauseDuration() * 1000L);
                }
                if (configService.isAudioAirRaidEnabled()) {
                    Thread.sleep(1500);
                    audioService.playAudioFile(configService.getAudioAirRaidPath());
                }
                addLog("Сигнал тривоги завершено. Режим ТРИВОГИ активовано.", "SUCCESS");
            } catch (InterruptedException e) {
                relayController.turnOff();
                currentAlertType = "NONE";
            } finally {
                isActionInProgress = false;
            }
        }).start();
    }

    public void runAirRaidClearSignal() {
        if (isActionInProgress) return;
        new Thread(() -> {
            isActionInProgress = true;
            currentAlertType = "NONE"; // Встановлюємо стан миттєво для UI
            addLog("ЗАПУСК СИГНАЛУ: ВІДБІЙ ПОВІТРЯНОЇ ТРИВОГИ", "SUCCESS");
            try {
                // Дзеркально повторюємо логіку тривоги: 3 цикли
                for (int i = 1; i <= 3; i++) {
                    relayController.turnOn();
                    Thread.sleep(configService.getAirRaidRingDuration() * 1000L);
                    relayController.turnOff();
                    if (i < 3) Thread.sleep(configService.getAirRaidPauseDuration() * 1000L);
                }
                addLog("Відбій тривоги завершено.", "SUCCESS");
            } catch (InterruptedException e) {
                relayController.turnOff();
            } finally {
                isActionInProgress = false;
            }
        }).start();
    }

    public void runEmergencySignal() {
        if (isActionInProgress) return;
        new Thread(() -> {
            isActionInProgress = true;
            currentAlertType = "EMERGENCY";
            addLog("ЗАПУСК СИГНАЛУ: НАДЗВИЧАЙНА СИТУАЦІЯ", "ERROR");
            try {
                relayController.turnOn();
                Thread.sleep(configService.getEmergencyDuration() * 1000L);
                relayController.turnOff();
                if (configService.isAudioEmergencyEnabled()) {
                    Thread.sleep(1500);
                    audioService.playAudioFile(configService.getAudioEmergencyPath());
                }
                addLog("Сигнал НС завершено. Режим НАДЗВИЧАЙНОЇ СИТУАЦІЇ активовано.", "SUCCESS");
            } catch (InterruptedException e) {
                relayController.turnOff();
                currentAlertType = "NONE";
            } finally {
                isActionInProgress = false;
            }
        }).start();
    }

    public void runEmergencyClearSignal() {
        if (isActionInProgress) return;
        new Thread(() -> {
            isActionInProgress = true;
            currentAlertType = "NONE"; // Встановлюємо стан миттєво для UI
            addLog("ЗАПУСК СИГНАЛУ: СКАСУВАННЯ ЕКСТРЕННОЇ СИТУАЦІЇ", "SUCCESS");
            try {
                // Дзеркально до сигналу НС: використовуємо налаштовану тривалість
                relayController.turnOn();
                Thread.sleep(configService.getEmergencyDuration() * 1000L);
                relayController.turnOff();
                addLog("Екстренну ситуацію скасовано.", "SUCCESS");
            } catch (InterruptedException e) {
                relayController.turnOff();
            } finally {
                isActionInProgress = false;
            }
        }).start();
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
                    currentAlertType = "NONE";
                }
            }
        }).start();
    }

    public void checkAndTriggerBell(LocalTime now, List<BellEntry> schedule) {
        if (now.getHour() == 9 && now.getMinute() == 0 && now.getSecond() == 0) {
            if (configService.isAudioSilenceEnabled()) {
                new Thread(() -> {
                    currentAlertType = "SILENCE";
                    audioService.playAudioFile(configService.getAudioSilencePath());
                    try {
                        Thread.sleep(65000); // 65 seconds
                    } catch (InterruptedException ignored) {
                    } finally {
                        if ("SILENCE".equals(currentAlertType)) {
                            currentAlertType = "NONE";
                        }
                    }
                }).start();
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
