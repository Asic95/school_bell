package com.schoolbell.model;

import java.time.LocalTime;

public record BellEntry(LocalTime time, int durationSeconds, String type) {
}
