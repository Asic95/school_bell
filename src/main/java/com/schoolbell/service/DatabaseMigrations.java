package com.schoolbell.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMigrations {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrations.class);

    public static void run(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            
            // --- BASE TABLES ---
            
            // Settings table
            stmt.execute("CREATE TABLE IF NOT EXISTS settings (" +
                    "key TEXT PRIMARY KEY," +
                    "value TEXT" +
                    ")");

            // Teachers table
            stmt.execute("CREATE TABLE IF NOT EXISTS teachers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL" +
                    ")");

            // Subjects table
            stmt.execute("CREATE TABLE IF NOT EXISTS subjects (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL" +
                    ")");

            // Teacher-Subject mapping
            stmt.execute("CREATE TABLE IF NOT EXISTS teacher_subjects (" +
                    "teacher_id INTEGER," +
                    "subject_id INTEGER," +
                    "PRIMARY KEY (teacher_id, subject_id)," +
                    "FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE" +
                    ")");

            // Classes table
            stmt.execute("CREATE TABLE IF NOT EXISTS classes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL" +
                    ")");

            // Schedule table
            stmt.execute("CREATE TABLE IF NOT EXISTS schedule (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "class_id INTEGER," +
                    "day_of_week INTEGER," +
                    "lesson_number INTEGER," +
                    "teacher_id INTEGER," +
                    "subject_id INTEGER," +
                    "classroom_id INTEGER DEFAULT 0," +
                    "parity INTEGER DEFAULT 0," +
                    "UNIQUE(class_id, day_of_week, lesson_number, parity)," +
                    "FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL," +
                    "FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL" +
                    ")");

            // Substitutions table
            stmt.execute("CREATE TABLE IF NOT EXISTS substitutions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "class_id INTEGER," +
                    "date TEXT NOT NULL," +
                    "lesson_number INTEGER," +
                    "teacher_id INTEGER," +
                    "subject_id INTEGER," +
                    "classroom_id INTEGER DEFAULT 0," +
                    "UNIQUE(class_id, date, lesson_number)," +
                    "FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL," +
                    "FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL" +
                    ")");
            
            // Broadcast Devices table
            stmt.execute("CREATE TABLE IF NOT EXISTS broadcast_devices (" +
                    "ip TEXT PRIMARY KEY," +
                    "name TEXT," +
                    "is_banned INTEGER DEFAULT 0," +
                    "device_type TEXT," +
                    "os TEXT," +
                    "last_seen TEXT" +
                    ")");

            // Announcements table
            stmt.execute("CREATE TABLE IF NOT EXISTS announcements (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "text TEXT NOT NULL," +
                    "start_date TEXT," +
                    "end_date TEXT," +
                    "start_time TEXT," +
                    "end_time TEXT," +
                    "days_of_week TEXT," +
                    "is_active INTEGER DEFAULT 1" +
                    ")");

            // Classrooms table
            stmt.execute("CREATE TABLE IF NOT EXISTS classrooms (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL" +
                    ")");

            // Media Events table
            stmt.execute("CREATE TABLE IF NOT EXISTS media_events (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "path TEXT NOT NULL," +
                    "type TEXT NOT NULL," +
                    "time TEXT," +
                    "days_of_week TEXT," +
                    "date TEXT," +
                    "is_active INTEGER DEFAULT 1," +
                    "is_folder INTEGER DEFAULT 0," +
                    "duration_minutes INTEGER DEFAULT 0," +
                    "break_anchor TEXT DEFAULT 'START'," +
                    "break_offset INTEGER DEFAULT 0" +
                    ")");

            // --- INCREMENTAL MIGRATIONS ---

            // Migration: Add break_anchor and break_offset to media_events if they don't exist
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "media_events", "break_anchor")) {
                if (!rs.next()) {
                    logger.info("Adding break_anchor and break_offset to media_events table...");
                    stmt.execute("ALTER TABLE media_events ADD COLUMN break_anchor TEXT DEFAULT 'START'");
                    stmt.execute("ALTER TABLE media_events ADD COLUMN break_offset INTEGER DEFAULT 0");
                }
            }

            // Migration: Check if we need to add new columns to broadcast_devices
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "broadcast_devices", "device_type")) {
                if (!rs.next()) {
                    logger.info("Migrating broadcast_devices table...");
                    stmt.execute("ALTER TABLE broadcast_devices ADD COLUMN device_type TEXT");
                    stmt.execute("ALTER TABLE broadcast_devices ADD COLUMN os TEXT");
                    stmt.execute("ALTER TABLE broadcast_devices ADD COLUMN last_seen TEXT");
                }
            }

            // Parity Multi-Support Migration
            try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, "schedule", true, false)) {
                boolean hasParityInUnique = false;
                while (rs.next()) {
                    if ("parity".equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                        hasParityInUnique = true;
                        break;
                    }
                }
                
                if (!hasParityInUnique) {
                    logger.info("Migrating schedule table to support multi-parity UNIQUE constraint...");
                    stmt.execute("BEGIN TRANSACTION");
                    stmt.execute("CREATE TABLE schedule_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "class_id INTEGER," +
                            "day_of_week INTEGER," +
                            "lesson_number INTEGER," +
                            "teacher_id INTEGER," +
                            "subject_id INTEGER," +
                            "parity INTEGER DEFAULT 0," +
                            "UNIQUE(class_id, day_of_week, lesson_number, parity)," +
                            "FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE," +
                            "FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL," +
                            "FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL" +
                            ")");
                    stmt.execute("INSERT INTO schedule_new (class_id, day_of_week, lesson_number, teacher_id, subject_id, parity) " +
                                 "SELECT class_id, day_of_week, lesson_number, teacher_id, subject_id, parity FROM schedule");
                    stmt.execute("DROP TABLE schedule");
                    stmt.execute("ALTER TABLE schedule_new RENAME TO schedule");
                    stmt.execute("COMMIT");
                    logger.info("Migration completed successfully.");
                }
            }

            // Migration: Add classroom_id to schedule if it doesn't exist
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "schedule", "classroom_id")) {
                if (!rs.next()) {
                    logger.info("Adding classroom_id to schedule table...");
                    stmt.execute("ALTER TABLE schedule ADD COLUMN classroom_id INTEGER DEFAULT 0");
                }
            }

            // Migration: Add classroom_id to substitutions if it doesn't exist
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "substitutions", "classroom_id")) {
                if (!rs.next()) {
                    logger.info("Adding classroom_id to substitutions table...");
                    stmt.execute("ALTER TABLE substitutions ADD COLUMN classroom_id INTEGER DEFAULT 0");
                }
            }
        }
    }
}
