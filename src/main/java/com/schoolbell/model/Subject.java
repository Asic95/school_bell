package com.schoolbell.model;

public record Subject(int id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
