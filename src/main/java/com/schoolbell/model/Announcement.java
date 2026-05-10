package com.schoolbell.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Announcement(
        int id,
        String text,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String daysOfWeek, // e.g., "1,2,3,4,5"
        boolean isActive
) {}
