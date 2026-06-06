package com.schoolbell.hardware;

import com.schoolbell.MainApp;
import com.schoolbell.service.NetworkDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Hybrid Manager for Relay Devices.
 * Switches between physical USB HID and Wireless Wi-Fi (Shelly) devices.
 */
public class RelayController {
    private static final Logger logger = LoggerFactory.getLogger(RelayController.class);
    
    private final MainApp mainApp;
    private RelayDevice activeDevice;
    private final NetworkDiscoveryService discoveryService;

    public RelayController(MainApp mainApp) {
        this.mainApp = mainApp;
        this.discoveryService = new NetworkDiscoveryService(mainApp);
        initializeDevice();
    }

    private void initializeDevice() {
        if (activeDevice != null) activeDevice.close();

        String type = mainApp.getConfigService().getRelayType(); // "USB" or "SHELLY"
        if ("SHELLY".equals(type)) {
            String ip = mainApp.getConfigService().getShellyIp();
            String name = mainApp.getConfigService().getShellyName();
            if (ip != null && !ip.isEmpty()) {
                activeDevice = new ShellyRelayDevice(mainApp, ip, name != null ? name : "Shelly Device");
            } else {
                // Return a dummy device that just says "not configured"
                activeDevice = new RelayDevice() {
                    @Override public void turnOn() {}
                    @Override public void turnOff() {}
                    @Override public boolean isConnected() { return false; }
                    @Override public String getDisplayName() { return "Shelly: Пристрій не налаштовано"; }
                    @Override public void close() {}
                };
            }
        } else {
            activeDevice = new UsbRelayDevice(mainApp);
        }
        logger.info("RelayController initialized with: {}", activeDevice.getDisplayName());
    }

    public void switchDevice(String type, String ip, String name) {
        mainApp.getConfigService().setRelayType(type);
        if ("SHELLY".equals(type)) {
            mainApp.getConfigService().setShellyIp(ip);
            mainApp.getConfigService().setShellyName(name);
        }
        initializeDevice();
    }

    public void turnOn() {
        if (activeDevice != null) activeDevice.turnOn();
    }

    public void turnOff() {
        if (activeDevice != null) activeDevice.turnOff();
    }

    public boolean isConnected() {
        return activeDevice != null && activeDevice.isConnected();
    }

    public String getConnectionDetails() {
        return activeDevice != null ? activeDevice.getDisplayName() : "Пристрій не налаштовано";
    }

    public NetworkDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public void close() {
        if (activeDevice != null) activeDevice.close();
    }

    // Compatibility methods for existing code
    public void scanDevices() {
        if (activeDevice instanceof UsbRelayDevice) {
            // Logic moved to UsbRelayDevice, but we can log for backward compatibility
            logger.info("USB scan initiated via active device.");
        }
    }
}
