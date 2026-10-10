package com.schoolbell.service;

import com.schoolbell.service.UpdateService.UpdateManifest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateServiceTest {
    private UpdateService updateService;

    @BeforeEach
    public void setup() {
        updateService = new UpdateService();
        updateService.clearSnooze();
    }

    @AfterEach
    public void tearDown() {
        updateService.clearSnooze();
    }

    @Test
    @DisplayName("Should detect newer versions correctly")
    public void testIsNewerVersion() {
        assertTrue(updateService.isNewerVersion("1.3.0"));
        assertTrue(updateService.isNewerVersion("1.2.8"));
        assertTrue(updateService.isNewerVersion("2.0.0"));

        assertFalse(updateService.isNewerVersion("1.2.7"));
        assertFalse(updateService.isNewerVersion("1.2.6"));
        assertFalse(updateService.isNewerVersion("1.1.0"));
        assertFalse(updateService.isNewerVersion(null));
    }

    @Test
    @DisplayName("Should snooze normal update and track count")
    public void testNormalUpdateSnooze() {
        UpdateManifest manifest = new UpdateManifest(
                "1.3.0",
                "2026-10-10",
                false,
                null,
                List.of("Виправлено помилки"),
                "https://example.com/setup.exe",
                "dummy-sha"
        );

        assertFalse(updateService.isUpdateSnoozed(manifest));
        assertEquals(0, updateService.getSnoozeCount("1.3.0"));

        // Snooze for 24 hours
        updateService.snoozeUpdate(manifest, 24 * 3600 * 1000L);

        assertTrue(updateService.isUpdateSnoozed(manifest));
        assertEquals(1, updateService.getSnoozeCount("1.3.0"));

        // Different manifest version should not be snoozed
        UpdateManifest nextManifest = new UpdateManifest(
                "1.4.0",
                "2026-10-11",
                false,
                null,
                List.of("Нові функції"),
                "https://example.com/setup2.exe",
                "dummy-sha-2"
        );
        assertFalse(updateService.isUpdateSnoozed(nextManifest));

        // Clear snooze
        updateService.clearSnooze();
        assertFalse(updateService.isUpdateSnoozed(manifest));
    }

    @Test
    @DisplayName("Critical update should cap snooze duration to at most 4 hours")
    public void testCriticalUpdateSnoozeCap() {
        UpdateManifest criticalManifest = new UpdateManifest(
                "1.3.0",
                "2026-10-10",
                true,
                "1.2.6",
                List.of("Критичне оновлення безпеки"),
                "https://example.com/setup.exe",
                "dummy-sha"
        );

        long before = System.currentTimeMillis();
        // Request 24h snooze, but critical must cap at 4h
        updateService.snoozeUpdate(criticalManifest, 24 * 3600 * 1000L);

        assertTrue(updateService.isUpdateSnoozed(criticalManifest));
        assertEquals(1, updateService.getSnoozeCount("1.3.0"));

        String untilStr = DatabaseManager.getSetting(UpdateService.SETTING_SNOOZED_UNTIL, "0");
        long until = Long.parseLong(untilStr);
        long maxExpectedUntil = before + (4 * 3600 * 1000L) + 2000L; // allow 2s margin

        assertTrue(until <= maxExpectedUntil, "Critical update snooze duration should not exceed 4 hours");
    }

    @Test
    @DisplayName("Should track download status and listeners properly")
    public void testDownloadStatusAndListeners() {
        assertEquals(UpdateService.DownloadStatus.IDLE, updateService.getDownloadStatus());
        assertEquals(0.0, updateService.getDownloadProgress());

        java.util.concurrent.atomic.AtomicReference<UpdateService.DownloadStatus> statusRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Double> progressRef = new java.util.concurrent.atomic.AtomicReference<>();

        java.util.function.Consumer<UpdateService.DownloadStatus> sListener = statusRef::set;
        java.util.function.Consumer<Double> pListener = progressRef::set;

        updateService.addStatusListener(sListener);
        updateService.addProgressListener(pListener);

        assertEquals(UpdateService.DownloadStatus.IDLE, statusRef.get());
        assertEquals(0.0, progressRef.get());

        // Invalid manifest should fail gracefully
        UpdateManifest invalidManifest = new UpdateManifest(
                "1.5.0",
                "2026-10-10",
                false,
                null,
                List.of(),
                null, // invalid URL
                null
        );

        var future = updateService.startBackgroundDownload(invalidManifest);
        assertTrue(future.isCompletedExceptionally());

        updateService.removeStatusListener(sListener);
        updateService.removeProgressListener(pListener);
    }
}
