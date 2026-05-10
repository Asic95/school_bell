package com.schoolbell.model;

import java.time.LocalDate;

public record SubstitutionEntry(int id, int classId, LocalDate date, int lessonNumber, int teacherId, int subjectId, int classroomId) {
}
