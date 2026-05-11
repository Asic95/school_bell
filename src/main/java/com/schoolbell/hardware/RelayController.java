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
            if (relayDevice.isClosed()) {
                relayDevice.open();
            }
            logger.info("Connected to HID Relay: {} (Serial: {})", 
                    relayDevice.getProduct(), relayDevice.getSerialNumber());
            return true;
        }
        return false;
    }

    public void turnOn() {
        // Команда 0xFE активує УСІ канали реле одночасно
        sendCommand((byte) 0xFE, (byte) 0x00); 
    }

    public void turnOff() {
        // Команда 0xFC деактивує УСІ канали реле одночасно
        sendCommand((byte) 0xFC, (byte) 0x00);
    }

    private void sendCommand(byte command, byte index) {
        String action = (command == (byte) 0xFE || command == (byte) 0xFF) ? "ON" : "OFF";
        
        if (mainApp != null && mainApp.getConfigService().isSimulationMode()) {
            logger.info("[SIMULATION] Relay command: {} to channel {}", action, index);
            return;
        }

        if (relayDevice == null || relayDevice.isClosed()) {
            if (!connect()) return;
        }

        // Пакет для реле: [command, index, 0, 0, 0, 0, 0, 0]
        byte[] featureData = new byte[8];
        featureData[0] = command;
        featureData[1] = index;
        featureData[2] = 0x00;
        featureData[3] = 0x00;
        featureData[4] = 0x00;
        featureData[5] = 0x00;
        featureData[6] = 0x00;
        featureData[7] = 0x00;
        
        // Використовуємо sendFeatureReport як основний метод, оскільки він підтвердив свою працездатність
        int val = relayDevice.sendFeatureReport(featureData, (byte) 0x00);
        
        if (val >= 0) {
            logger.info("Relay command sent (FeatureReport): {} to channel {}", action, index);
        } else {
            logger.warn("FeatureReport failed, trying legacy write: {}", relayDevice.getLastErrorMessage());
            
            // Запасний варіант: звичайний запис (Output Report)
            byte[] report = new byte[9];
            report[0] = 0x00; // Report ID
            System.arraycopy(featureData, 0, report, 1, 8);
            
            int writeVal = relayDevice.write(report, 9, (byte) 0x00);
            if (writeVal >= 0) {
                logger.info("Relay command sent (Legacy Write): {} to channel {}", action, index);
            } else {
                logger.error("All relay communication attempts failed. Error: {}", relayDevice.getLastErrorMessage());
            }
        }
    }

    private com.schoolbell.MainApp mainApp;
    public void setMainApp(com.schoolbell.MainApp mainApp) { this.mainApp = mainApp; }

    public boolean isConnected() {
        if (mainApp != null && mainApp.getConfigService().isSimulationMode()) return true;
        // Dynamic check: look for any matching device in the current attached list
        List<HidDevice> devices = hidServices.getAttachedHidDevices();
        boolean found = devices.stream()
                .anyMatch(d -> d.getVendorId() == VENDOR_ID && d.getProductId() == PRODUCT_ID);
        
        // If device is found but we haven't 'opened' our local relayDevice handle, try to connect
        if (found && (relayDevice == null || relayDevice.isClosed())) {
            connect();
        }
        
        return found && relayDevice != null && !relayDevice.isClosed();
    }

    public String getConnectionDetails() {
        if (relayDevice != null && !relayDevice.isClosed()) {
            return String.format("USB HID: VID_%04X PID_%04X | %s", 
                    VENDOR_ID, PRODUCT_ID, relayDevice.getProduct() != null ? relayDevice.getProduct() : "Relay Module");
        }
        return "Пристрій не знайдено в системі";
    }

    public void close() {
        if (relayDevice != null && !relayDevice.isClosed()) {
            relayDevice.close();
        }
        hidServices.stop();
    }
}
