package com.schoolbell.hardware;

import com.schoolbell.MainApp;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UsbRelayDevice implements RelayDevice {
    private static final Logger logger = LoggerFactory.getLogger(UsbRelayDevice.class);
    
    private static final int VENDOR_ID = 0x16c0; 
    private static final int PRODUCT_ID = 0x05df;

    private final HidServices hidServices;
    private HidDevice relayDevice;
    private final MainApp mainApp;

    public UsbRelayDevice(MainApp mainApp) {
        this.mainApp = mainApp;
        this.hidServices = HidManager.getHidServices();
    }

    private boolean connect() {
        if (mainApp != null && mainApp.getConfigService().isSimulationMode()) return true;
        
        hidServices.start();
        Optional<HidDevice> device = hidServices.getAttachedHidDevices().stream()
                .filter(d -> d.getVendorId() == VENDOR_ID && d.getProductId() == PRODUCT_ID)
                .findFirst();

        if (device.isPresent()) {
            relayDevice = device.get();
            if (relayDevice.isClosed()) {
                relayDevice.open();
            }
            logger.info("Connected to HID Relay: {} (Serial: {})", 
                    relayDevice.getProduct(), relayDevice.getSerialNumber());
            return true;
        }
        return false;
    }

    @Override
    public void turnOn() {
        sendCommand((byte) 0xFE, (byte) 0x00); 
    }

    @Override
    public void turnOff() {
        sendCommand((byte) 0xFC, (byte) 0x00);
    }

    private void sendCommand(byte command, byte index) {
        String action = (command == (byte) 0xFE || command == (byte) 0xFF) ? "ON" : "OFF";
        
        if (mainApp != null && mainApp.getConfigService().isSimulationMode()) {
            logger.info("[SIMULATION] USB Relay command: {}", action);
            return;
        }

        if (relayDevice == null || relayDevice.isClosed()) {
            if (!connect()) return;
        }

        byte[] featureData = new byte[8];
        featureData[0] = command;
        featureData[1] = index;
        
        int val = relayDevice.sendFeatureReport(featureData, (byte) 0x00);
        if (val >= 0) {
            logger.info("USB Relay command sent: {}", action);
        } else {
            // Legacy write fallback
            byte[] report = new byte[9];
            report[0] = 0x00;
            System.arraycopy(featureData, 0, report, 1, 8);
            if (relayDevice.write(report, 9, (byte) 0x00) >= 0) {
                logger.info("USB Relay command sent (Legacy Write): {}", action);
            } else {
                logger.error("USB Relay communication failed: {}", relayDevice.getLastErrorMessage());
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (mainApp != null && mainApp.getConfigService().isSimulationMode()) return true;
        List<HidDevice> devices = hidServices.getAttachedHidDevices();
        boolean found = devices.stream()
                .anyMatch(d -> d.getVendorId() == VENDOR_ID && d.getProductId() == PRODUCT_ID);
        
        if (found && (relayDevice == null || relayDevice.isClosed())) {
            connect();
        }
        return found && relayDevice != null && !relayDevice.isClosed();
    }

    @Override
    public String getDisplayName() {
        if (isConnected()) {
            if (mainApp != null && mainApp.getConfigService().isSimulationMode()) return "USB HID (СИМУЛЯЦІЯ)";
            return String.format("USB HID: VID_%04X PID_%04X | %s", 
                    VENDOR_ID, PRODUCT_ID, relayDevice.getProduct() != null ? relayDevice.getProduct() : "Relay Module");
        }
        return "USB Реле не підключено";
    }

    @Override
    public void close() {
        if (relayDevice != null && !relayDevice.isClosed()) {
            relayDevice.close();
        }
        hidServices.stop();
    }
}
