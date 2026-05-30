package com.schoolbell.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PathService {
    private static final Logger logger = LoggerFactory.getLogger(PathService.class);
    private static final String APP_FOLDER = "SchoolBell";
    private static final String DB_NAME = "school_bell.db";

    public static String getAppHomePath() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            // Fallback to user home if APPDATA is not set (e.g. non-windows, though we are targeting win)
            appData = System.getProperty("user.home");
        }
        File home = new File(appData, APP_FOLDER);
        if (!home.exists()) {
            home.mkdirs();
        }
        return home.getAbsolutePath();
    }

    public static String getDatabasePath() {
        return new File(getAppHomePath(), DB_NAME).getAbsolutePath();
    }

    public static String getDatabaseUrl() {
        return "jdbc:sqlite:" + getDatabasePath();
    }

    /**
     * Migrates the database from the local directory to AppData if necessary.
     */
    public static void migrateIfNeeded() {
        File localDb = new File(DB_NAME);
        File targetDb = new File(getDatabasePath());

        if (localDb.exists() && !targetDb.exists()) {
            try {
                logger.info("Migrating database from local folder to: " + targetDb.getAbsolutePath());
                Files.copy(localDb.toPath(), targetDb.toPath(), StandardCopyOption.REPLACE_EXISTING);
                // We keep the old file as a backup for now, but rename it
                localDb.renameTo(new File(DB_NAME + ".bak"));
                logger.info("Migration successful.");
            } catch (Exception e) {
                logger.error("Failed to migrate database to AppData", e);
            }
        }
    }
}
