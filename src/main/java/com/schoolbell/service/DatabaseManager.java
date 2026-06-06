package com.schoolbell.service;

import com.schoolbell.model.BroadcastDevice;
import com.schoolbell.model.MediaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = PathService.getDatabaseUrl();

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
        PathService.migrateIfNeeded();
        try (Connection conn = getConnection()) {
            DatabaseMigrations.run(conn);
            logger.info("Database initialized successfully.");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
        }
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

    public static List<BroadcastDevice> getAllBroadcastDevices() {
        List<BroadcastDevice> devices = new ArrayList<>();
        String sql = "SELECT ip, name, is_banned, device_type, os, last_seen FROM broadcast_devices";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                devices.add(new BroadcastDevice(
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

    public static void saveBroadcastDevice(BroadcastDevice device) {
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

    public static List<MediaEvent> getAllMediaEvents() {
        List<MediaEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM media_events";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                events.add(new MediaEvent(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("path"),
                        rs.getString("type"),
                        rs.getString("time"),
                        rs.getString("days_of_week"),
                        rs.getString("date"),
                        rs.getInt("is_active") == 1,
                        rs.getInt("is_folder") == 1,
                        rs.getInt("duration_minutes"),
                        rs.getString("break_anchor"),
                        rs.getInt("break_offset")
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to load media events", e);
        }
        return events;
    }

    public static void saveMediaEvent(MediaEvent event) {
        String sql;
        if (event.id() == null) {
            sql = "INSERT INTO media_events (name, path, type, time, days_of_week, date, is_active, is_folder, duration_minutes, break_anchor, break_offset) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE media_events SET name=?, path=?, type=?, time=?, days_of_week=?, date=?, is_active=?, is_folder=?, duration_minutes=?, break_anchor=?, break_offset=? WHERE id=?";
        }
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, event.name());
            pstmt.setString(2, event.path());
            pstmt.setString(3, event.type());
            pstmt.setString(4, event.time());
            pstmt.setString(5, event.daysOfWeek());
            pstmt.setString(6, event.date());
            pstmt.setInt(7, event.isActive() ? 1 : 0);
            pstmt.setInt(8, event.isFolder() ? 1 : 0);
            pstmt.setInt(9, event.durationMinutes());
            pstmt.setString(10, event.breakAnchor() != null ? event.breakAnchor() : "START");
            pstmt.setInt(11, event.breakOffset());
            if (event.id() != null) pstmt.setInt(12, event.id());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save media event", e);
        }
    }

    public static void deleteMediaEvent(int id) {
        String sql = "DELETE FROM media_events WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete media event: " + id, e);
        }
    }

    // --- RADIO STATIONS ---

    public static List<com.schoolbell.model.RadioStation> getAllRadioStations() {
        List<com.schoolbell.model.RadioStation> stations = new ArrayList<>();
        String sql = "SELECT * FROM radio_stations ORDER BY name ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stations.add(new com.schoolbell.model.RadioStation(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("url"),
                        rs.getString("favicon_url")
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to load radio stations", e);
        }
        return stations;
    }

    public static void saveRadioStation(com.schoolbell.model.RadioStation station) {
        String sql;
        if (station.id() == null) {
            sql = "INSERT INTO radio_stations (name, url, favicon_url) VALUES (?, ?, ?)";
        } else {
            sql = "UPDATE radio_stations SET name=?, url=?, favicon_url=? WHERE id=?";
        }
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, station.name());
            pstmt.setString(2, station.url());
            pstmt.setString(3, station.faviconUrl());
            if (station.id() != null) pstmt.setInt(4, station.id());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save radio station", e);
        }
    }

    public static void deleteRadioStation(int id) {
        String sql = "DELETE FROM radio_stations WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete radio station: " + id, e);
        }
    }

    public static void saveSystemLog(String level, String message) {
        String sql = "INSERT INTO system_logs (level, message, timestamp) VALUES (?, ?, datetime('now', 'localtime'))";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, level);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save system log", e);
        }
    }

    public static List<String> getSystemLogs(int limitDays) {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT strftime('%d.%m.%Y %H:%M:%S', timestamp) as ts_formatted, level, message FROM system_logs " +
                     "WHERE timestamp > date('now', 'localtime', ?) " +
                     "ORDER BY timestamp DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "-" + limitDays + " days");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String ts = rs.getString("ts_formatted");
                    String level = rs.getString("level");
                    String msg = rs.getString("message");
                    logs.add("[" + ts + "] [" + level + "] " + msg);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load system logs", e);
        }
        return logs;
    }

    /**
     * Cleans up old data:
     * - Substitutions older than 45 days (by 'date' column)
     * - Announcements older than 45 days (by 'end_date' column)
     * - System logs older than 7 days
     */
    public static void cleanupOldData() {
        String cleanupSubstitutions = "DELETE FROM substitutions WHERE date < date('now', '-45 days')";
        String cleanupAnnouncements = "DELETE FROM announcements WHERE end_date < date('now', '-45 days')";
        String cleanupLogs = "DELETE FROM system_logs WHERE timestamp < date('now', '-7 days')";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            int subsCount = stmt.executeUpdate(cleanupSubstitutions);
            int annCount = stmt.executeUpdate(cleanupAnnouncements);
            int logsCount = stmt.executeUpdate(cleanupLogs);
            
            if (subsCount > 0 || annCount > 0 || logsCount > 0) {
                logger.info("Database cleanup completed. Removed {} subs, {} ann, {} logs.", subsCount, annCount, logsCount);
            }
        } catch (SQLException e) {
            logger.error("Failed to cleanup old data from database", e);
        }
    }
}
