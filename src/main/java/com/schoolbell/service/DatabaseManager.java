package com.schoolbell.service;

import com.schoolbell.model.BroadcastDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

    public static BroadcastDevice getDeviceByIp(String ip) {
        return getAllBroadcastDevices().stream()
                .filter(d -> d.ip().equals(ip))
                .findFirst()
                .orElse(null);
    }

    public static void saveSetting(String key, String value) {
        String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save setting: " + key, e);
        }
    }

    public static String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load setting: " + key, e);
        }
        return defaultValue;
    }

    public static java.util.List<com.schoolbell.model.BroadcastDevice> getAllBroadcastDevices() {
        java.util.List<com.schoolbell.model.BroadcastDevice> devices = new java.util.ArrayList<>();
        String sql = "SELECT ip, name, is_banned, device_type, os, last_seen FROM broadcast_devices";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                devices.add(new com.schoolbell.model.BroadcastDevice(
                        rs.getString("ip"),
                        rs.getString("name"),
                        rs.getInt("is_banned") == 1,
                        rs.getString("device_type"),
                        rs.getString("os"),
                        rs.getString("last_seen")
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to load broadcast devices", e);
        }
        return devices;
    }

    public static void saveBroadcastDevice(com.schoolbell.model.BroadcastDevice device) {
        String sql = "INSERT OR REPLACE INTO broadcast_devices (ip, name, is_banned, device_type, os, last_seen) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, device.ip());
            pstmt.setString(2, device.name());
            pstmt.setInt(3, device.isBanned() ? 1 : 0);
            pstmt.setString(4, device.deviceType());
            pstmt.setString(5, device.os());
            pstmt.setString(6, device.lastSeen());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save broadcast device: " + device.ip(), e);
        }
    }

    public static void deleteBroadcastDevice(String ip) {
        String sql = "DELETE FROM broadcast_devices WHERE ip = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ip);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete broadcast device: " + ip, e);
        }
    }

    public static void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
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

            // Migration: Check if we need to add new columns to broadcast_devices
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "broadcast_devices", "device_type")) {
                if (!rs.next()) {
                    logger.info("Migrating broadcast_devices table...");
                    stmt.execute("ALTER TABLE broadcast_devices ADD COLUMN device_type TEXT");
                    stmt.execute("ALTER TABLE broadcast_devices ADD COLUMN os TEXT");
                    stmt.execute("ALTER TABLE broadcast_devices ADD COLUMN last_seen TEXT");
                }
            } catch (Exception e) {
                logger.warn("Broadcast devices migration skipped: " + e.getMessage());
            }

            // ... rest of initialize ...
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

            // Migration: Add classroom_id to schedule if it doesn't exist
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "schedule", "classroom_id")) {
                if (!rs.next()) {
                    logger.info("Adding classroom_id to schedule table...");
                    stmt.execute("ALTER TABLE schedule ADD COLUMN classroom_id INTEGER DEFAULT 0");
                }
            } catch (Exception e) {
                logger.warn("Schedule classroom_id migration skipped: " + e.getMessage());
            }

            // Migration: Add classroom_id to substitutions if it doesn't exist
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "substitutions", "classroom_id")) {
                if (!rs.next()) {
                    logger.info("Adding classroom_id to substitutions table...");
                    stmt.execute("ALTER TABLE substitutions ADD COLUMN classroom_id INTEGER DEFAULT 0");
                }
            } catch (Exception e) {
                logger.warn("Substitutions classroom_id migration skipped: " + e.getMessage());
            }

            logger.info("Database initialized successfully.");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
        }
    }
}
