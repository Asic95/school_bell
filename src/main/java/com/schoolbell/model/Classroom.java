package com.schoolbell.model;

public record Classroom(int id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
