package com.schoolbell;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class SystemJournal {
    private static final Logger logger = LoggerFactory.getLogger(SystemJournal.class);
    private final ObservableList<String> systemLogs = FXCollections.observableArrayList();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void addLog(String message, String level) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        String fullMsg = "[" + timestamp + "] [" + level + "] " + message;
        logger.info(fullMsg);
        
        // Save to DB
        com.schoolbell.service.DatabaseManager.saveSystemLog(level, message);
        
        Platform.runLater(() -> {
            systemLogs.add(0, fullMsg);
            if (systemLogs.size() > 100) systemLogs.remove(100, systemLogs.size());
        });
    }

    public void loadLastLogs() {
        java.util.List<String> logs = com.schoolbell.service.DatabaseManager.getSystemLogs(1); // Last 24 hours
        Platform.runLater(() -> {
            systemLogs.clear();
            // DB returns newest first, which is what we want for add(0, ...)
            systemLogs.addAll(logs);
        });
    }

    public ObservableList<String> getSystemLogs() {
        return systemLogs;
    }
}
