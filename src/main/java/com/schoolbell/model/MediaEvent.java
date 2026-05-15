package com.schoolbell.model;

import java.io.Serializable;
import java.util.List;

public record MediaEvent(
    Integer id,
    String name,
    String path,
    String type, // "TIME", "BREAKS", "ONCE"
    String time, // "HH:mm" or empty
    String daysOfWeek, // "1,2,3,4,5" (1=Mon, 7=Sun)
    String date, // "yyyy-MM-dd" or empty
    boolean isActive,
    boolean isFolder,
    int durationMinutes, // 0 for full file
    String breakAnchor, // "START", "END", "MIDDLE"
    int breakOffset // minutes
) implements Serializable {}
