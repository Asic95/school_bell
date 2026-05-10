package com.schoolbell.service;

import com.schoolbell.model.Announcement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementService {
    private static final Logger logger = LoggerFactory.getLogger(AnnouncementService.class);

    public List<Announcement> getAllAnnouncements() {
        List<Announcement> announcements = new ArrayList<>();
        String sql = "SELECT * FROM announcements ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                announcements.add(mapResultSetToAnnouncement(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting announcements", e);
        }
        return announcements;
    }

    public void addAnnouncement(Announcement a) {
        String sql = "INSERT INTO announcements (text, start_date, end_date, start_time, end_time, days_of_week, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, a.text());
            pstmt.setString(2, a.startDate() != null ? a.startDate().toString() : null);
            pstmt.setString(3, a.endDate() != null ? a.endDate().toString() : null);
            pstmt.setString(4, a.startTime() != null ? a.startTime().toString() : null);
            pstmt.setString(5, a.endTime() != null ? a.endTime().toString() : null);
            pstmt.setString(6, a.daysOfWeek());
            pstmt.setInt(7, a.isActive() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding announcement", e);
        }
    }

    public void updateAnnouncement(Announcement a) {
        String sql = "UPDATE announcements SET text = ?, start_date = ?, end_date = ?, start_time = ?, end_time = ?, days_of_week = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, a.text());
            pstmt.setString(2, a.startDate() != null ? a.startDate().toString() : null);
            pstmt.setString(3, a.endDate() != null ? a.endDate().toString() : null);
            pstmt.setString(4, a.startTime() != null ? a.startTime().toString() : null);
            pstmt.setString(5, a.endTime() != null ? a.endTime().toString() : null);
            pstmt.setString(6, a.daysOfWeek());
            pstmt.setInt(7, a.isActive() ? 1 : 0);
            pstmt.setInt(8, a.id());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating announcement", e);
        }
    }

    public void deleteAnnouncement(int id) {
        String sql = "DELETE FROM announcements WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting announcement", e);
        }
    }

    public String getActiveAnnouncementText(LocalDate date, LocalTime time) {
        String dayOfWeek = String.valueOf(date.getDayOfWeek().getValue());
        String sql = "SELECT text FROM announcements WHERE is_active = 1 " +
                "AND (start_date IS NULL OR start_date <= ?) " +
                "AND (end_date IS NULL OR end_date >= ?) " +
                "AND (start_time IS NULL OR start_time <= ?) " +
                "AND (end_time IS NULL OR end_time >= ?) " +
                "AND (days_of_week IS NULL OR days_of_week = '' OR days_of_week LIKE ?)";
        
        java.util.StringJoiner joiner = new java.util.StringJoiner("   •   ");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.toString());
            pstmt.setString(2, date.toString());
            pstmt.setString(3, time.toString());
            pstmt.setString(4, time.toString());
            pstmt.setString(5, "%" + dayOfWeek + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    joiner.add(rs.getString("text"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting active announcements", e);
        }
        return joiner.length() > 0 ? joiner.toString() : null;
    }

    private Announcement mapResultSetToAnnouncement(ResultSet rs) throws SQLException {
        return new Announcement(
                rs.getInt("id"),
                rs.getString("text"),
                rs.getString("start_date") != null ? LocalDate.parse(rs.getString("start_date")) : null,
                rs.getString("end_date") != null ? LocalDate.parse(rs.getString("end_date")) : null,
                rs.getString("start_time") != null ? LocalTime.parse(rs.getString("start_time")) : null,
                rs.getString("end_time") != null ? LocalTime.parse(rs.getString("end_time")) : null,
                rs.getString("days_of_week"),
                rs.getInt("is_active") == 1
        );
    }
}
