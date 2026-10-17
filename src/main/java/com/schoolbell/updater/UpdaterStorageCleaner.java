package com.schoolbell.updater;

import com.schoolbell.service.PathService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Periodically cleans up the shared updater storage:
 * - Stale .part files from interrupted downloads (> 2 hours old)
 * - Obsolete installer .exe files that do not match the current/active update
 * - Stale .event.part and expired .event files (> 7 days old)
 * - Rotated log file updater.log.1 (> 14 days old or excessive size)
 *
 * Guarantees that active installers and live locks are never touched.
 */
public final class UpdaterStorageCleaner {
    private static final Duration PART_FILE_MAX_AGE = Duration.ofHours(2);
    private static final Duration EVENT_PART_MAX_AGE = Duration.ofHours(1);
    private static final Duration EVENT_FILE_MAX_AGE = Duration.ofDays(7);
    private static final Duration ROTATED_LOG_MAX_AGE = Duration.ofDays(14);
    private static final long ROTATED_LOG_MAX_SIZE = 2 * 1024 * 1024; // 2 MB

    private UpdaterStorageCleaner() {}

    public record CleanResult(
            int deletedInstallers,
            int deletedPartFiles,
            int deletedEventFiles,
            int deletedLogFiles,
            long freedBytes
    ) {
        public int totalDeleted() {
            return deletedInstallers + deletedPartFiles + deletedEventFiles + deletedLogFiles;
        }
    }

    public static CleanResult cleanStorage(Path activeInstallerToKeep, Consumer<String> logger) {
        return cleanStorage(PathService.getSharedUpdaterDirectory(), activeInstallerToKeep, logger);
    }

    /**
     * Cleans the updater storage in the specified root directory without deleting the active installer.
     *
     * @param rootDir updater root directory (containing downloads/ and events/)
     * @param activeInstallerToKeep path to the active or downloaded installer that must not be deleted (may be null)
     * @param logger optional callback for logging actions (may be null)
     * @return summary of deleted files and freed bytes
     */
    public static CleanResult cleanStorage(Path rootDir, Path activeInstallerToKeep, Consumer<String> logger) {
        Consumer<String> log = logger != null ? logger : msg -> {};
        Path normalizedActiveInstaller = activeInstallerToKeep != null
                ? activeInstallerToKeep.toAbsolutePath().normalize()
                : null;

        int deletedInstallers = 0;
        int deletedPartFiles = 0;
        int deletedEventFiles = 0;
        int deletedLogFiles = 0;
        long freedBytes = 0;

        Instant now = Instant.now();

        // 1. Clean downloads directory
        Path downloadsDir = rootDir != null ? rootDir.resolve("downloads") : PathService.getSharedUpdaterDownloadsDirectory();
        if (Files.isDirectory(downloadsDir)) {
            try (Stream<Path> stream = Files.list(downloadsDir)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString().toLowerCase();

                    // 1.1 Stale .part download files
                    if (name.endsWith(".part")) {
                        if (isOlderThan(file, now, PART_FILE_MAX_AGE)) {
                            long size = getFileSize(file);
                            if (deleteSilently(file)) {
                                deletedPartFiles++;
                                freedBytes += size;
                                log.accept("Видалено застарілий тимчасовий файл завантаження: " + file.getFileName());
                            }
                        }
                    }
                    // 1.2 Installer EXEs
                    else if (name.endsWith(".exe")) {
                        Path normalizedFile = file.toAbsolutePath().normalize();
                        if (normalizedActiveInstaller != null && normalizedFile.equals(normalizedActiveInstaller)) {
                            // Never delete active/verified installer
                            continue;
                        }
                        long size = getFileSize(file);
                        if (deleteSilently(file)) {
                            deletedInstallers++;
                            freedBytes += size;
                            log.accept("Видалено застарілий інсталятор: " + file.getFileName());
                        }
                    }
                }
            } catch (IOException e) {
                log.accept("Помилка при скануванні downloads: " + e.getMessage());
            }
        }

        // 2. Clean events directory
        Path eventsDir = rootDir != null ? rootDir.resolve("events") : PathService.getSharedUpdaterEventsDirectory();
        if (Files.isDirectory(eventsDir)) {
            try (Stream<Path> stream = Files.list(eventsDir)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString().toLowerCase();

                    // 2.1 Stale event parts
                    if (name.endsWith(".event.part")) {
                        if (isOlderThan(file, now, EVENT_PART_MAX_AGE)) {
                            long size = getFileSize(file);
                            if (deleteSilently(file)) {
                                deletedEventFiles++;
                                freedBytes += size;
                                log.accept("Видалено залишковий тимчасовий файл події: " + file.getFileName());
                            }
                        }
                    }
                    // 2.2 Expired events that were never imported
                    else if (name.endsWith(".event")) {
                        if (isOlderThan(file, now, EVENT_FILE_MAX_AGE)) {
                            long size = getFileSize(file);
                            if (deleteSilently(file)) {
                                deletedEventFiles++;
                                freedBytes += size;
                                log.accept("Видалено застарілу неімпортовану подію: " + file.getFileName());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.accept("Помилка при скануванні events: " + e.getMessage());
            }
        }

        // 3. Clean root updater directory (rotated log files)
        Path cleanRootDir = rootDir != null ? rootDir : PathService.getSharedUpdaterDirectory();
        if (Files.isDirectory(cleanRootDir)) {
            Path rotatedLog = cleanRootDir.resolve("updater.log.1");
            if (Files.isRegularFile(rotatedLog)) {
                if (isOlderThan(rotatedLog, now, ROTATED_LOG_MAX_AGE) || getFileSize(rotatedLog) > ROTATED_LOG_MAX_SIZE) {
                    long size = getFileSize(rotatedLog);
                    if (deleteSilently(rotatedLog)) {
                        deletedLogFiles++;
                        freedBytes += size;
                        log.accept("Видалено застарілий ротований журнал: updater.log.1");
                    }
                }
            }
        }

        CleanResult result = new CleanResult(deletedInstallers, deletedPartFiles, deletedEventFiles, deletedLogFiles, freedBytes);
        if (result.totalDeleted() > 0) {
            log.accept(String.format("Очищення updater-сховища завершено: видалено %d файлів, звільнено %.2f MB",
                    result.totalDeleted(), (double) freedBytes / (1024 * 1024)));
        }
        return result;
    }

    private static boolean isOlderThan(Path file, Instant now, Duration maxAge) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            Instant modified = attrs.lastModifiedTime().toInstant();
            return Duration.between(modified, now).compareTo(maxAge) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static long getFileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    private static boolean deleteSilently(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (Exception e) {
            return false;
        }
    }
}
