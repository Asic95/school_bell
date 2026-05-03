package com.schoolbell.model;

public record Teacher(int id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
