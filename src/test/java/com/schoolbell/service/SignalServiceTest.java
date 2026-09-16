package com.schoolbell.service;

import com.schoolbell.hardware.RelayController;
import com.schoolbell.hardware.RelayDevice;
import com.schoolbell.model.BellEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SignalServiceTest {

    private TestRelayDevice testRelayDevice;
    private RelayController relayController;
    private TestAudioService testAudioService;
    private ConfigService configService;
    private SignalService signalService;

    static class TestRelayDevice implements RelayDevice {
        final AtomicInteger turnOnCount = new AtomicInteger(0);
        final AtomicInteger turnOffCount = new AtomicInteger(0);

        @Override public void turnOn() { turnOnCount.incrementAndGet(); }
        @Override public void turnOff() { turnOffCount.incrementAndGet(); }
        @Override public boolean isConnected() { return true; }
        @Override public String getDisplayName() { return "Test Relay"; }
        @Override public void close() {}
    }

    static class TestAudioService extends AudioService {
        final List<String> playedFiles = new CopyOnWriteArrayList<>();

        public TestAudioService(ConfigService configService) {
            super(configService);
        }

        @Override
        public void playAudioFile(String path, String displayName) {
            if (path != null) {
                playedFiles.add(path);
            }
        }

        @Override
        public void playPlaylist(List<File> files, boolean loop) {
            if (files != null) {
                for (File f : files) {
                    playedFiles.add(f.getAbsolutePath());
                }
            }
        }
    }

    @BeforeEach
    void setUp() {
        testRelayDevice = new TestRelayDevice();
        relayController = new RelayController(testRelayDevice);
        configService = new ConfigService();
        configService.setAudioSilenceEnabled(true);
        configService.setAudioSilencePath("C:/test_audio/silence.mp3");
        configService.setRegularBellDuration(1);
        configService.setAirRaidRingDuration(1);
        configService.setAirRaidPauseDuration(1);

        testAudioService = new TestAudioService(configService);
        signalService = new SignalService(relayController, testAudioService, configService);
    }

    @Test
    @DisplayName("Audio 'хвилина мовчання' MUST trigger at 09:00:00 even when air raid alert is active")
    void testSilenceMinuteTriggersDuringAirRaid() throws InterruptedException {
        // Activate air raid signal
        signalService.runAirRaidSignal();

        // Wait for initial air raid siren cycle to complete
        int waitCount = 0;
        while (signalService.isActionInProgress() && waitCount++ < 50) {
            Thread.sleep(100);
        }
        assertTrue(signalService.isAirRaidActive(), "Air raid must be marked active");
        assertEquals("AIR_RAID", signalService.getCurrentAlertType());

        int relayOnBefore = testRelayDevice.turnOnCount.get();

        // Check 09:00:00 trigger with a schedule that also has a lesson start at 09:00:00
        List<BellEntry> schedule = new ArrayList<>();
        schedule.add(new BellEntry(LocalTime.of(9, 0), 5, "1 урок (початок)"));

        signalService.checkAndTriggerBell(LocalTime.of(9, 0, 0), schedule);

        // Wait for silence audio to be dispatched
        int attempts = 0;
        while (testAudioService.playedFiles.isEmpty() && attempts++ < 30) {
            Thread.sleep(100);
        }

        // Verify that silence audio was played!
        assertFalse(testAudioService.playedFiles.isEmpty(), "Audio for silence minute must be triggered");
        assertTrue(testAudioService.playedFiles.contains("C:/test_audio/silence.mp3"),
                "Expected silence audio path to be played even during air raid");

        // Verify relay did NOT ring for regular 9:00 lesson bell
        assertEquals(relayOnBefore, testRelayDevice.turnOnCount.get(),
                "Relay must NOT ring for scheduled lesson bell at 09:00 when air raid is active");

        // Air raid state must NOT be lost
        assertTrue(signalService.isAirRaidActive(), "Air raid must remain active during and after silence trigger");
    }

    @Test
    @DisplayName("Regular scheduled bells must remain blocked when air raid is active")
    void testRegularBellsBlockedDuringAirRaid() throws InterruptedException {
        signalService.runAirRaidSignal();
        Thread.sleep(100);

        int relayOnBefore = testRelayDevice.turnOnCount.get();

        List<BellEntry> schedule = List.of(
                new BellEntry(LocalTime.of(9, 45), 5, "1 урок (кінець)")
        );

        // Trigger at 09:45:00 during air raid
        signalService.checkAndTriggerBell(LocalTime.of(9, 45, 0), schedule);
        Thread.sleep(200);

        // No new relay turnOn should happen for regular bell
        // (any count difference could only be from the air raid siren itself if still in loop)
        // Let's verify by testing when action in progress is false
        int waitAttempts = 0;
        while (signalService.isActionInProgress() && waitAttempts++ < 20) {
            Thread.sleep(500);
        }

        int stableRelayCount = testRelayDevice.turnOnCount.get();
        signalService.checkAndTriggerBell(LocalTime.of(9, 45, 0), schedule);
        Thread.sleep(200);

        assertEquals(stableRelayCount, testRelayDevice.turnOnCount.get(),
                "Scheduled bell must NOT ring relay when air raid is active");
    }

    @Test
    @DisplayName("Silence minute triggers in peacetime and sets SILENCE alert type")
    void testSilenceMinuteInPeacetime() throws InterruptedException {
        assertFalse(signalService.isAirRaidActive());

        List<BellEntry> schedule = new ArrayList<>();
        signalService.checkAndTriggerBell(LocalTime.of(9, 0, 0), schedule);

        Thread.sleep(300);

        assertTrue(testAudioService.playedFiles.contains("C:/test_audio/silence.mp3"));
        assertEquals("SILENCE", signalService.getCurrentAlertType());
    }
}
