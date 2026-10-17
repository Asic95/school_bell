package com.schoolbell.updater;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UpdaterStorageCleanerTest {

    @Test
    @DisplayName("Should preserve active installer while deleting obsolete versions")
    public void testPreserveActiveInstallerAndRemoveOldInstallers(@TempDir Path tempDir) throws IOException {
        Path downloadsDir = tempDir.resolve("downloads");
        Files.createDirectories(downloadsDir);

        Path oldInstaller = downloadsDir.resolve("SchoolBell_Setup_v1.2.6.exe");
        Path activeInstaller = downloadsDir.resolve("SchoolBell_Setup_v1.2.8.exe");

        Files.writeString(oldInstaller, "old version binary content");
        Files.writeString(activeInstaller, "active version binary content");

        List<String> logs = new ArrayList<>();
        UpdaterStorageCleaner.CleanResult result = UpdaterStorageCleaner.cleanStorage(tempDir, activeInstaller, logs::add);

        assertEquals(1, result.deletedInstallers());
        assertFalse(Files.exists(oldInstaller), "Old installer should have been deleted");
        assertTrue(Files.exists(activeInstaller), "Active installer must be preserved");
    }

    @Test
    @DisplayName("Should remove stale .part files and preserve recently modified .part files")
    public void testStalePartFilesCleanedAndFreshPreserved(@TempDir Path tempDir) throws IOException {
        Path downloadsDir = tempDir.resolve("downloads");
        Files.createDirectories(downloadsDir);

        Path stalePart = downloadsDir.resolve("SchoolBell_Setup_v1.2.9.exe.part");
        Path freshPart = downloadsDir.resolve("SchoolBell_Setup_v1.3.0.exe.part");

        Files.writeString(stalePart, "abandoned partial download");
        Files.writeString(freshPart, "active in-progress download");

        // Set stalePart modified time to 3 hours ago
        Instant threeHoursAgo = Instant.now().minus(3, ChronoUnit.HOURS);
        Files.setLastModifiedTime(stalePart, FileTime.from(threeHoursAgo));

        // Set freshPart modified time to 10 minutes ago
        Instant tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        Files.setLastModifiedTime(freshPart, FileTime.from(tenMinutesAgo));

        UpdaterStorageCleaner.CleanResult result = UpdaterStorageCleaner.cleanStorage(tempDir, null, null);

        assertEquals(1, result.deletedPartFiles());
        assertFalse(Files.exists(stalePart), "Stale .part file older than 2 hours should be deleted");
        assertTrue(Files.exists(freshPart), "Fresh .part file should not be deleted");
    }

    @Test
    @DisplayName("Should clean stale event parts and expired events, keeping fresh events")
    public void testStaleEventPartAndOldEventFiles(@TempDir Path tempDir) throws IOException {
        Path eventsDir = tempDir.resolve("events");
        Files.createDirectories(eventsDir);

        Path staleEventPart = eventsDir.resolve("uuid-1.event.part");
        Path expiredEvent = eventsDir.resolve("uuid-2.event");
        Path freshEvent = eventsDir.resolve("uuid-3.event");

        Files.writeString(staleEventPart, "broken event");
        Files.writeString(expiredEvent, "INFO\nold message");
        Files.writeString(freshEvent, "INFO\nfresh message");

        Files.setLastModifiedTime(staleEventPart, FileTime.from(Instant.now().minus(2, ChronoUnit.HOURS)));
        Files.setLastModifiedTime(expiredEvent, FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS)));
        Files.setLastModifiedTime(freshEvent, FileTime.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        UpdaterStorageCleaner.CleanResult result = UpdaterStorageCleaner.cleanStorage(tempDir, null, null);

        assertEquals(2, result.deletedEventFiles());
        assertFalse(Files.exists(staleEventPart), "Stale .event.part should be deleted");
        assertFalse(Files.exists(expiredEvent), "Event older than 7 days should be deleted");
        assertTrue(Files.exists(freshEvent), "Fresh event must be preserved");
    }

    @Test
    @DisplayName("Should delete expired updater.log.1 while preserving current updater.log")
    public void testRotatedLogCleaned(@TempDir Path tempDir) throws IOException {
        Path currentLog = tempDir.resolve("updater.log");
        Path rotatedLog = tempDir.resolve("updater.log.1");

        Files.writeString(currentLog, "[2026-10-10] Current log entry");
        Files.writeString(rotatedLog, "[2026-09-01] Old rotated log entry");

        Files.setLastModifiedTime(rotatedLog, FileTime.from(Instant.now().minus(20, ChronoUnit.DAYS)));

        UpdaterStorageCleaner.CleanResult result = UpdaterStorageCleaner.cleanStorage(tempDir, null, null);

        assertEquals(1, result.deletedLogFiles());
        assertFalse(Files.exists(rotatedLog), "Old updater.log.1 should be deleted");
        assertTrue(Files.exists(currentLog), "Current updater.log must never be deleted");
    }
}
