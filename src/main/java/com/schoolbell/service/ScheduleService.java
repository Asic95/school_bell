package com.schoolbell.service;

import com.schoolbell.model.BellEntry;
import com.schoolbell.model.DaySchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);
    private static final int DEFAULT_DURATION = 5;

    public List<DaySchedule> loadInternalSchedules() {
        List<DaySchedule> schedules = new ArrayList<>();
        
        // Try loading from DB
        try (Connection conn = DatabaseManager.getConnection()) {
            String sqlSchedules = "SELECT id, name FROM bell_schedules";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSchedules)) {
                
                while (rs.next()) {
                    int scheduleId = rs.getInt("id");
                    String name = rs.getString("name");
                    DaySchedule ds = new DaySchedule(name);
                    List<DaySchedule.LessonInfo> lessons = new ArrayList<>();
                    
                    String sqlLessons = "SELECT start_time, end_time, break_duration FROM bell_lessons WHERE schedule_id = ? ORDER BY lesson_number";
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlLessons)) {
                        pstmt.setInt(1, scheduleId);
                        try (ResultSet rsL = pstmt.executeQuery()) {
                            while (rsL.next()) {
                                DaySchedule.LessonInfo li = new DaySchedule.LessonInfo();
                                String start = rsL.getString("start_time");
                                String end = rsL.getString("end_time");
                                if (start != null && !start.isEmpty()) li.start = LocalTime.parse(start);
                                if (end != null && !end.isEmpty()) li.end = LocalTime.parse(end);
                                li.breakAfterMinutes = rsL.getInt("break_duration");
                                lessons.add(li);
                            }
                        }
                    }
                    ds.setLessons(lessons);
                    schedules.add(ds);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load schedules from database", e);
        }

        // If DB is empty, create a default schedule
        if (schedules.isEmpty()) {
            // Completely fresh start
            DaySchedule defaultSch = new DaySchedule("Стандартний");
            schedules.add(defaultSch);
            saveInternalSchedules(schedules);
        }

        return schedules;
    }

    public void saveInternalSchedules(List<DaySchedule> schedules) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Clear existing
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM bell_lessons");
                    stmt.execute("DELETE FROM bell_schedules");
                }

                String sqlSch = "INSERT INTO bell_schedules (name) VALUES (?)";
                String sqlLes = "INSERT INTO bell_lessons (schedule_id, lesson_number, start_time, end_time, break_duration) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement pstmtSch = conn.prepareStatement(sqlSch, Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement pstmtLes = conn.prepareStatement(sqlLes)) {
                    
                    for (DaySchedule ds : schedules) {
                        pstmtSch.setString(1, ds.getName());
                        pstmtSch.executeUpdate();
                        int scheduleId;
                        try (ResultSet rs = pstmtSch.getGeneratedKeys()) {
                            if (rs.next()) {
                                scheduleId = rs.getInt(1);
                            } else {
                                throw new SQLException("Failed to get generated key for schedule");
                            }
                        }

                        List<DaySchedule.LessonInfo> lessons = ds.getLessons();
                        for (int i = 0; i < lessons.size(); i++) {
                            DaySchedule.LessonInfo li = lessons.get(i);
                            pstmtLes.setInt(1, scheduleId);
                            pstmtLes.setInt(2, i + 1);
                            pstmtLes.setString(3, li.start != null ? li.start.toString() : null);
                            pstmtLes.setString(4, li.end != null ? li.end.toString() : null);
                            pstmtLes.setInt(5, li.breakAfterMinutes);
                            pstmtLes.executeUpdate();
                        }
                    }
                }
                conn.commit();
                logger.info("Schedules saved to database successfully.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Failed to save schedules to database", e);
        }
    }

    public List<BellEntry> convertToBellEntries(DaySchedule daySchedule) {
        List<BellEntry> entries = new ArrayList<>();
        int lessonNum = 1;
        for (DaySchedule.LessonInfo li : daySchedule.getLessons()) {
            if (li.start != null) {
                entries.add(new BellEntry(li.start, DEFAULT_DURATION, "IN: " + lessonNum + " урок (початок)"));
            }
            if (li.end != null) {
                entries.add(new BellEntry(li.end, DEFAULT_DURATION, "OUT: " + lessonNum + " урок (кінець)"));
            }
            lessonNum++;
        }
        entries.sort((a, b) -> a.time().compareTo(b.time()));
        return entries;
    }
}
