package com.schoolbell.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:sqlite:school_bell.db";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC driver not found", e);
            throw new SQLException("SQLite driver not found", e);
        }
        return DriverManager.getConnection(DB_URL);
    }

    public static void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
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

            // Teacher-Subject mapping (many-to-many)
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

            // Weekly Schedule table
            stmt.execute("CREATE TABLE IF NOT EXISTS schedule (" +
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

            // Migration: Check if we need to update the UNIQUE constraint
            // SQLite doesn't support ALTER TABLE for constraints, so we need a recreation approach
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
            } catch (Exception e) {
                logger.warn("Parity migration skipped or already done: " + e.getMessage());
            }

            logger.info("Database initialized successfully.");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
        }
    }
}
