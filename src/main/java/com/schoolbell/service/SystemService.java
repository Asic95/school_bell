package com.schoolbell.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.schoolbell.ui.RestoreConfirmationDialog;

public class SystemService {
    private static final Logger logger = LoggerFactory.getLogger(SystemService.class);
    private final ConfigService config;

    public SystemService(ConfigService config) {
        this.config = config;
    }

    /**
     * Shows a file chooser and copies the database file to the selected location.
     */
    public boolean createDatabaseBackup(Stage owner) {
        File dbFile = new File("school_bell.db");
        if (!dbFile.exists()) {
            logger.error("Database file not found for backup: " + dbFile.getAbsolutePath());
            return false;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Зберегти резервну копію бази даних");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        fileChooser.setInitialFileName("school_bell_backup_" + timestamp + ".db");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файли бази даних SQLite", "*.db"));

        File destFile = fileChooser.showSaveDialog(owner);
        if (destFile != null) {
            try {
                Files.copy(dbFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Database backup created successfully: " + destFile.getAbsolutePath());
                return true;
            } catch (IOException e) {
                logger.error("Failed to create database backup", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Updates Windows Registry to enable or disable autostart.
     */
    public void updateAutostart(boolean enable) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return;

        try {
            String runningPath = getRunningExecutablePath();
            if (runningPath == null) {
                logger.warn("Could not determine running path, skipping autostart update.");
                return;
            }

            String command;
            if (enable) {
                String appCommand;
                if (runningPath.endsWith(".exe")) {
                    // Running as a packaged EXE
                    appCommand = "\"" + runningPath + "\"";
                } else if (runningPath.endsWith(".jar")) {
                    // Running as a standalone JAR
                    appCommand = "javaw -jar \"" + runningPath + "\"";
                } else {
                    logger.warn("Not running from a JAR or EXE, skipping autostart update: " + runningPath);
                    return;
                }
                
                command = "reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v \"SchoolBell\" /t REG_SZ /d \"" + appCommand + "\" /f";
            } else {
                command = "reg delete \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v \"SchoolBell\" /f";
            }

            executeCommand(command);
            logger.info("Autostart " + (enable ? "enabled" : "disabled") + " for path: " + runningPath);
        } catch (Exception e) {
            logger.error("Failed to update autostart", e);
        }
    }

    /**
     * Adds a Windows Firewall rule to allow incoming traffic on the broadcast port.
     * Uses PowerShell elevation to trigger a UAC prompt if needed.
     */
    public void optimizeFirewall(int port) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return;

        try {
            // We use PowerShell to run netsh commands with elevation (-Verb RunAs)
            // This will show the standard Windows "Do you want to allow this app..." prompt
            
            String deleteCommand = "netsh advfirewall firewall delete rule name=\\\"SchoolBell Web Dashboard\\\"";
            String addCommand = String.format(
                "netsh advfirewall firewall add rule name=\\\"SchoolBell Web Dashboard\\\" dir=in action=allow protocol=TCP localport=%d",
                port
            );

            // The script combines both commands
            String script = String.format(
                "Start-Process cmd.exe -ArgumentList '/c %s & %s' -Verb RunAs -WindowStyle Hidden",
                deleteCommand, addCommand
            );
            
            String[] command = {"powershell.exe", "-NoProfile", "-Command", script};
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            
            logger.info("Firewall optimization requested via UAC for port " + port);
        } catch (Exception e) {
            logger.error("Failed to trigger firewall optimization", e);
        }
    }

    /**
     * Checks if a firewall rule for the given port exists and is enabled.
     */
    public boolean isPortAllowedInFirewall(int port) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return true;

        try {
            // netsh might return localized output, so we check for common success markers
            // and verify that the port is mentioned in the rule.
            String output = executeCommandWithOutput("netsh advfirewall firewall show rule name=\"SchoolBell Web Dashboard\"");
            
            if (output == null || output.isEmpty()) return false;

            boolean isEnabled = output.contains("Yes") || output.contains("Так") || output.contains("Да");
            boolean isPortMatch = output.contains(String.valueOf(port));
            
            return isEnabled && isPortMatch;
        } catch (Exception e) {
            logger.error("Error checking firewall status", e);
            return false;
        }
    }

    private String getRunningExecutablePath() {
        try {
            // 1. Try to get the actual EXE path (Java 9+)
            String command = ProcessHandle.current().info().command().orElse(null);
            if (command != null && command.endsWith(".exe") && !command.toLowerCase().contains("java")) {
                return new File(command).getAbsolutePath();
            }

            // 2. Fallback to JAR path detection
            String path = SystemService.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            if (decodedPath.startsWith("/")) decodedPath = decodedPath.substring(1);
            File file = new File(decodedPath);
            
            // 3. If we are running a JAR, check if there's an EXE with the same name in the parent folder (jpackage style)
            if (file.getName().endsWith(".jar")) {
                File parent = file.getParentFile();
                if (parent != null && parent.getName().equals("app")) {
                    File grandParent = parent.getParentFile();
                    if (grandParent != null) {
                        File exe = new File(grandParent, "SchoolBell.exe");
                        if (exe.exists()) return exe.getAbsolutePath();
                    }
                }
            }
            
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private void executeCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.warn("Command failed with exit code " + exitCode + ": " + command);
        }
    }

    /**
     * Sets the Windows system volume (0-100).
     */
    public void setWindowsSystemVolume(int level) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return;
        
        try {
            // Using a clever PowerShell trick: maximize volume (50 steps of 2%) then decrease to desired
            // Or even simpler: use the SndVol tool if possible, but PowerShell is more reliable.
            // Precision is tricky with SendKeys, but it's the most compatible way without external DLLs.
            int stepsToMax = 50;
            int stepsDown = (100 - level) / 2;
            
            String script = String.format(
                "$w = New-Object -ComObject WScript.Shell; " +
                "for($i=0; $i -lt %d; $i++) { $w.SendKeys([char]175) }; " + // Vol Up
                "for($i=0; $i -lt %d; $i++) { $w.SendKeys([char]174) }",    // Vol Down
                stepsToMax, stepsDown
            );
            
            String[] command = {"powershell.exe", "-NoProfile", "-Command", script};
            Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            logger.error("Failed to set system volume", e);
        }
    }

    private String executeCommandWithOutput(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP866"))) { // Windows console encoding
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        process.waitFor();
        return output.toString();
    }

    /**
     * Shows a file chooser to select a backup and restores it.
     */
    public boolean restoreDatabaseBackup(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Оберіть файл резервної копії для відновлення");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файли бази даних SQLite", "*.db"));

        File backupFile = fileChooser.showOpenDialog(owner);
        if (backupFile != null) {
            RestoreConfirmationDialog dialog = new RestoreConfirmationDialog(owner);
            dialog.display();

            if (dialog.isConfirmed()) {
                File dbFile = new File("school_bell.db");
                try {
                    // Overwrite the current database file
                    Files.copy(backupFile.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Database restored successfully from: " + backupFile.getAbsolutePath());
                    return true;
                } catch (IOException e) {
                    logger.error("Failed to restore database backup", e);
                    return false;
                }
            }
        }
        return false;
    }
}
