package com.schoolbell.model;

public record SchoolClass(int id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
