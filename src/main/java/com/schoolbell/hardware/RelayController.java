package com.schoolbell.hardware;

import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class RelayController {
    private static final Logger logger = LoggerFactory.getLogger(RelayController.class);
    
    // Default VIDs/PIDs for the relay from the provided docs
    private static final int VENDOR_ID = 0x16c0; 
    private static final int PRODUCT_ID = 0x05df;

    private final HidServices hidServices;
    private HidDevice relayDevice;

    public RelayController() {
        this.hidServices = HidManager.getHidServices();
    }

    public void scanDevices() {
        hidServices.start();
        List<HidDevice> devices = hidServices.getAttachedHidDevices();
        logger.info("--- HID Device Scan (Total: {}) ---", devices.size());
        for (HidDevice dev : devices) {
            logger.info("Device Found: VID=0x{}, PID=0x{}, Mfr={}, Product={}, Serial={}",
                    Integer.toHexString(dev.getVendorId()),
                    Integer.toHexString(dev.getProductId()),
                    dev.getManufacturer(),
                    dev.getProduct(),
                    dev.getSerialNumber());
        }
        logger.info("----------------------------------");
    }

    public boolean connect() {
        hidServices.start();
        
        Optional<HidDevice> device = hidServices.getAttachedHidDevices().stream()
                .filter(d -> d.getVendorId() == VENDOR_ID && d.getProductId() == PRODUCT_ID)
                .findFirst();

        if (device.isPresent()) {
            relayDevice = device.get();
            if (!relayDevice.isOpen()) {
                relayDevice.open();
            }
            logger.info("Connected to HID Relay: {} (Serial: {})", 
                    relayDevice.getProduct(), relayDevice.getSerialNumber());
            return true;
        }
        return false;
    }

    public void turnOn() {
        // Official protocol: 0xFF, index, 0, 0, 0, 0, 0, 0
        sendCommand((byte) 0xFF, (byte) 0x01); 
    }

    public void turnOff() {
        // Official protocol: 0xFD, index, 0, 0, 0, 0, 0, 0
        sendCommand((byte) 0xFD, (byte) 0x01);
    }

    private void sendCommand(byte command, byte index) {
        if (relayDevice == null || !relayDevice.isOpen()) {
            if (!connect()) return;
        }

        byte[] report = new byte[9]; // 0th byte is Report ID
        report[0] = 0x00;
        report[1] = command;
        report[2] = index;
        report[3] = 0x00;
        report[4] = 0x00;
        report[5] = 0x00;
        report[6] = 0x00;
        report[7] = 0x00;
        report[8] = 0x00;
        
        int val = relayDevice.write(report, 8, (byte) 0x00);
        if (val >= 0) {
            logger.info("Relay command sent: {} to channel {}", 
                    (command == (byte) 0xFF ? "ON" : "OFF"), index);
        } else {
            logger.error("Relay write failed: {}", relayDevice.getLastErrorMessage());
        }
    }

    public boolean isConnected() {
        // Dynamic check: look for any matching device in the current attached list
        List<HidDevice> devices = hidServices.getAttachedHidDevices();
        boolean found = devices.stream()
                .anyMatch(d -> d.getVendorId() == VENDOR_ID && d.getProductId() == PRODUCT_ID);
        
        // If device is found but we haven't 'opened' our local relayDevice handle, try to connect
        if (found && (relayDevice == null || !relayDevice.isOpen())) {
            connect();
        }
        
        return found && relayDevice != null && relayDevice.isOpen();
    }

    public void close() {
        if (relayDevice != null && relayDevice.isOpen()) {
            relayDevice.close();
        }
        hidServices.stop();
    }
}
