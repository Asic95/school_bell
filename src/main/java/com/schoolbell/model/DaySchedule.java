package com.schoolbell.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DaySchedule {
    private String name;
    private List<LessonInfo> lessons = new ArrayList<>();

    public DaySchedule(String name) {
        this.name = name;
        for (int i = 0; i < 7; i++) {
            lessons.add(new LessonInfo());
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<LessonInfo> getLessons() { return lessons; }
    public void setLessons(List<LessonInfo> lessons) { this.lessons = lessons; }

    public static class LessonInfo {
        public LocalTime start;
        public LocalTime end;
        public int breakAfterMinutes;

        public LessonInfo() {}
        public LessonInfo(LocalTime start, LocalTime end, int breakAfter) {
            this.start = start;
            this.end = end;
            this.breakAfterMinutes = breakAfter;
        }
    }
}
