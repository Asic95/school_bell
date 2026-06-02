package com.schoolbell.hardware;

/**
 * Common interface for all relay execution devices.
 * Supports both physical (USB) and wireless (Wi-Fi) hardware.
 */
public interface RelayDevice {
    void turnOn();
    void turnOff();
    boolean isConnected();
    String getDisplayName();
    void close();
}
