package com.schoolbell.service;

import com.schoolbell.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AcademicService {
    private static final Logger logger = LoggerFactory.getLogger(AcademicService.class);

    // --- Classes ---
    public List<SchoolClass> getAllClasses() {
        List<SchoolClass> classes = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM classes")) {
            while (rs.next()) {
                classes.add(new SchoolClass(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            logger.error("Error getting classes", e);
        }

        classes.sort((c1, c2) -> {
            String n1 = c1.name();
            String n2 = c2.name();
            String num1 = n1.replaceAll("([^0-9]+.*)", "");
            String num2 = n2.replaceAll("([^0-9]+.*)", "");
            if (!num1.isEmpty() && !num2.isEmpty()) {
                int i1 = Integer.parseInt(num1);
                int i2 = Integer.parseInt(num2);
                if (i1 != i2) return Integer.compare(i1, i2);
            }
            return n1.compareToIgnoreCase(n2);
        });
        return classes;
    }

    public void addClass(String name) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO classes (name) VALUES (?)")) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding class", e);
        }
    }

    public void updateClass(int id, String name) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE classes SET name = ? WHERE id = ?")) {
            pstmt.setString(1, name);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating class", e);
        }
    }

    public void deleteClass(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM classes WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting class", e);
        }
    }

    // --- Classrooms ---
    public List<Classroom> getAllClassrooms() {
        List<Classroom> classrooms = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM classrooms ORDER BY name")) {
            while (rs.next()) {
                classrooms.add(new Classroom(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            logger.error("Error getting classrooms", e);
        }
        return classrooms;
    }

    public void addClassroom(String name) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO classrooms (name) VALUES (?)")) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding classroom", e);
        }
    }

    public void updateClassroom(int id, String name) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE classrooms SET name = ? WHERE id = ?")) {
            pstmt.setString(1, name);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating classroom", e);
        }
    }

    public void deleteClassroom(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM classrooms WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting classroom", e);
        }
    }

    // --- Schedule ---
    public List<ScheduleEntry> getScheduleForClass(int classId) {
        List<ScheduleEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM schedule WHERE class_id = ?")) {
            pstmt.setInt(1, classId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new ScheduleEntry(
                            rs.getInt("id"),
                            rs.getInt("class_id"),
                            rs.getInt("day_of_week"),
                            rs.getInt("lesson_number"),
                            rs.getInt("teacher_id"),
                            rs.getInt("subject_id"),
                            rs.getInt("classroom_id"),
                            rs.getInt("parity")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting schedule for class", e);
        }
        return entries;
    }

    public void saveScheduleEntry(int classId, int dayOfWeek, int lessonNumber, int teacherId, int subjectId, int classroomId, int parity) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (parity == 0) {
                    try (PreparedStatement del = conn.prepareStatement(
                            "DELETE FROM schedule WHERE class_id = ? AND day_of_week = ? AND lesson_number = ? AND parity IN (1, 2)")) {
                        del.setInt(1, classId);
                        del.setInt(2, dayOfWeek);
                        del.setInt(3, lessonNumber);
                        del.executeUpdate();
                    }
                } else {
                    try (PreparedStatement del = conn.prepareStatement(
                            "DELETE FROM schedule WHERE class_id = ? AND day_of_week = ? AND lesson_number = ? AND parity = 0")) {
                        del.setInt(1, classId);
                        del.setInt(2, dayOfWeek);
                        del.setInt(3, lessonNumber);
                        del.executeUpdate();
                    }
                }

                String sql = "INSERT OR REPLACE INTO schedule (class_id, day_of_week, lesson_number, teacher_id, subject_id, classroom_id, parity) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, classId);
                    pstmt.setInt(2, dayOfWeek);
                    pstmt.setInt(3, lessonNumber);
                    pstmt.setInt(4, teacherId);
                    pstmt.setInt(5, subjectId);
                    pstmt.setInt(6, classroomId);
                    pstmt.setInt(7, parity);
                    pstmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error saving schedule entry", e);
        }
    }

    public void deleteScheduleEntry(int classId, int dayOfWeek, int lessonNumber, int parity) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM schedule WHERE class_id = ? AND day_of_week = ? AND lesson_number = ? AND parity = ?")) {
            pstmt.setInt(1, classId);
            pstmt.setInt(2, dayOfWeek);
            pstmt.setInt(3, lessonNumber);
            pstmt.setInt(4, parity);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting schedule entry", e);
        }
    }

    // --- Substitutions ---
    public List<SubstitutionEntry> getAllSubstitutions() {
        List<SubstitutionEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM substitutions ORDER BY date DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                entries.add(new SubstitutionEntry(
                        rs.getInt("id"),
                        rs.getInt("class_id"),
                        java.time.LocalDate.parse(rs.getString("date")),
                        rs.getInt("lesson_number"),
                        rs.getInt("teacher_id"),
                        rs.getInt("subject_id"),
                        rs.getInt("classroom_id")
                ));
            }
        } catch (SQLException e) {
            logger.error("Error getting all substitutions", e);
        }
        return entries;
    }

    public List<SubstitutionEntry> getSubstitutionsForDate(java.time.LocalDate date) {
        List<SubstitutionEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM substitutions WHERE date = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new SubstitutionEntry(
                            rs.getInt("id"),
                            rs.getInt("class_id"),
                            java.time.LocalDate.parse(rs.getString("date")),
                            rs.getInt("lesson_number"),
                            rs.getInt("teacher_id"),
                            rs.getInt("subject_id"),
                            rs.getInt("classroom_id")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting substitutions", e);
        }
        return entries;
    }

    public void saveSubstitution(int classId, java.time.LocalDate date, int lessonNumber, int teacherId, int subjectId, int classroomId) {
        String sql = "INSERT OR REPLACE INTO substitutions (class_id, date, lesson_number, teacher_id, subject_id, classroom_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, classId);
            pstmt.setString(2, date.toString());
            pstmt.setInt(3, lessonNumber);
            pstmt.setInt(4, teacherId);
            pstmt.setInt(5, subjectId);
            pstmt.setInt(6, classroomId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error saving substitution", e);
        }
    }

    public void deleteSubstitution(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM substitutions WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting substitution", e);
        }
    }
}
