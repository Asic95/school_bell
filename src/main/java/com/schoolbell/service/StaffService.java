package com.schoolbell.service;

import com.schoolbell.model.Subject;
import com.schoolbell.model.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StaffService {
    private static final Logger logger = LoggerFactory.getLogger(StaffService.class);

    // --- Teachers ---
    public List<Teacher> getAllTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM teachers ORDER BY name")) {
            while (rs.next()) {
                teachers.add(new Teacher(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            logger.error("Error getting teachers", e);
        }
        return teachers;
    }

    public void addTeacher(String name) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO teachers (name) VALUES (?)")) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding teacher", e);
        }
    }

    public void updateTeacher(int id, String name) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE teachers SET name = ? WHERE id = ?")) {
            pstmt.setString(1, name);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating teacher", e);
        }
    }

    public void deleteTeacher(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM teachers WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting teacher", e);
        }
    }

    // --- Subjects ---
    public List<Subject> getAllSubjects() {
        List<Subject> subjects = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM subjects ORDER BY name")) {
            while (rs.next()) {
                subjects.add(new Subject(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            logger.error("Error getting subjects", e);
        }
        return subjects;
    }

    public void addSubject(String name) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO subjects (name) VALUES (?)")) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding subject", e);
        }
    }

    public void updateSubject(int id, String name) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE subjects SET name = ? WHERE id = ?")) {
            pstmt.setString(1, name);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating subject", e);
        }
    }

    public void deleteSubject(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM subjects WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting subject", e);
        }
    }

    // --- Teacher-Subject Mapping ---
    public List<Subject> getSubjectsForTeacher(int teacherId) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT s.* FROM subjects s JOIN teacher_subjects ts ON s.id = ts.subject_id WHERE ts.teacher_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teacherId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    subjects.add(new Subject(rs.getInt("id"), rs.getString("name")));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting subjects for teacher", e);
        }
        return subjects;
    }

    public void linkTeacherToSubject(int teacherId, int subjectId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT OR IGNORE INTO teacher_subjects (teacher_id, subject_id) VALUES (?, ?)")) {
            pstmt.setInt(1, teacherId);
            pstmt.setInt(2, subjectId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error linking teacher to subject", e);
        }
    }

    public void unlinkTeacherFromSubject(int teacherId, int subjectId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM teacher_subjects WHERE teacher_id = ? AND subject_id = ?")) {
            pstmt.setInt(1, teacherId);
            pstmt.setInt(2, subjectId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error unlinking teacher from subject", e);
        }
    }
}
