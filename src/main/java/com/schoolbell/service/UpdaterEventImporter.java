package com.schoolbell.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Imports events written by the SYSTEM updater into the current user's journal. */
public final class UpdaterEventImporter {
    private static final Logger logger = LoggerFactory.getLogger(UpdaterEventImporter.class);

    private UpdaterEventImporter() {}

    public static void importPendingEvents() {
        Path eventsDirectory = PathService.getSharedUpdaterEventsDirectory();
        if (!Files.isDirectory(eventsDirectory)) return;

        try (Stream<Path> files = Files.list(eventsDirectory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".event"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(UpdaterEventImporter::importEvent);
        } catch (IOException e) {
            logger.warn("Не вдалося прочитати події updater-а: {}", e.getMessage());
        }
    }

    private static void importEvent(Path eventFile) {
        try {
            List<String> lines = Files.readAllLines(eventFile, StandardCharsets.UTF_8);
            if (lines.size() < 2) {
                Files.deleteIfExists(eventFile);
                return;
            }

            String level = lines.get(0).trim();
            String message = String.join(System.lineSeparator(), lines.subList(1, lines.size()));
            if (!level.equals("INFO") && !level.equals("WARNING") && !level.equals("ERROR")) {
                level = "INFO";
            }

            DatabaseManager.saveSystemLog(level, message);
            Files.deleteIfExists(eventFile);
        } catch (Exception e) {
            logger.warn("Не вдалося імпортувати подію updater-а '{}': {}",
                    eventFile.getFileName(), e.getMessage());
        }
    }
}
