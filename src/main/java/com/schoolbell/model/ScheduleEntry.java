package com.schoolbell.model;

public record ScheduleEntry(int id, int classId, int dayOfWeek, int lessonNumber, int teacherId, int subjectId, int classroomId, int parity) {
}
