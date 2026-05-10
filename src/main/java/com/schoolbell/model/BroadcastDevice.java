package com.schoolbell.model;

public record BroadcastDevice(
    String ip, 
    String name, 
    boolean isBanned, 
    String deviceType, // PC, MOBILE, TABLET, UNKNOWN
    String os,         // Windows, Android, iOS, etc.
    String lastSeen    // Timestamp
) {
    public BroadcastDevice(String ip, String name, boolean isBanned) {
        this(ip, name, isBanned, "UNKNOWN", "Unknown", "Never");
    }
}
