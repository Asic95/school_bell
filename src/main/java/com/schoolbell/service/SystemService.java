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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.schoolbell.ui.RestoreConfirmationDialog;

public class SystemService {
    private static final Logger logger = LoggerFactory.getLogger(SystemService.class);
    private final ConfigService config;
    private final BiConsumer<String, String> journalConsumer;

    public record AutostartResult(boolean success, String message) {}

    public SystemService(ConfigService config) {
        this(config, (message, level) -> {});
    }

    public SystemService(ConfigService config, BiConsumer<String, String> journalConsumer) {
        this.config = config;
        this.journalConsumer = journalConsumer != null ? journalConsumer : (message, level) -> {};
    }

    /**
     * Shows a file chooser and copies the database file to the selected location.
     */
    public boolean createDatabaseBackup(Stage owner) {
        File dbFile = new File(PathService.getDatabasePath());
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
    public AutostartResult updateAutostart(boolean enable) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return new AutostartResult(false, "Автозапуск доступний лише у Windows.");
        }

        try {
            String runningPath = getRunningExecutablePath();
            if (runningPath == null) {
                logger.warn("Could not determine running path, skipping autostart update.");
                return new AutostartResult(false, "Не вдалося визначити шлях до програми.");
            }

            List<String> command = new ArrayList<>();
            command.add("reg.exe");
            if (enable) {
                String appCommand;
                if (runningPath.toLowerCase().endsWith(".exe")) {
                    // Running as a packaged EXE
                    appCommand = "\"" + runningPath + "\"";
                } else if (runningPath.toLowerCase().endsWith(".jar")) {
                    // Running as a standalone JAR
                    File javaw = new File(System.getProperty("java.home"), "bin\\javaw.exe");
                    String javawPath = javaw.exists() ? javaw.getAbsolutePath() : "javaw.exe";
                    appCommand = "\"" + javawPath + "\" -jar \"" + runningPath + "\"";
                } else {
                    logger.warn("Not running from a JAR or EXE, skipping autostart update: " + runningPath);
                    return new AutostartResult(false,
                            "Програму запущено не з готового EXE або JAR-файлу. " +
                                    "Автозапуск недоступний у режимі розробки (шлях: " + runningPath + ").");
                }

                command.addAll(List.of(
                        "add",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "SchoolBell",
                        "/t", "REG_SZ",
                        "/d", appCommand,
                        "/f"
                ));
            } else {
                command.addAll(List.of(
                        "delete",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "SchoolBell",
                        "/f"
                ));
            }

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append(System.lineSeparator());
                }
                output = result.toString().trim();
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Autostart update failed with exit code {}: {}", exitCode, output);
                return new AutostartResult(false,
                        "Windows не прийняв зміни автозапуску (код помилки: " + exitCode + ").");
            }

            logger.info("Autostart " + (enable ? "enabled" : "disabled") + " for path: " + runningPath);
            return new AutostartResult(true,
                    enable ? "Автозапуск разом із Windows увімкнено." : "Автозапуск разом із Windows вимкнено.");
        } catch (Exception e) {
            logger.error("Failed to update autostart", e);
            return new AutostartResult(false, "Не вдалося виконати команду Windows: " + e.getMessage());
        }
    }

    /**
     * Adds a Windows Firewall rule to allow incoming traffic on the broadcast port.
     * Uses PowerShell elevation to trigger a UAC prompt if needed.
     */
    public void optimizeFirewall(int port) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return;

        try {
            int websocketPort = port + 2;
            // We use PowerShell to run netsh commands with elevation (-Verb RunAs)
            // This will show the standard Windows "Do you want to allow this app..." prompt
            
            String deleteCommand = "netsh advfirewall firewall delete rule name=\\\"SchoolBell Web Dashboard\\\"";
            String addCommand = String.format(
                "netsh advfirewall firewall add rule name=\\\"SchoolBell Web Dashboard\\\" dir=in action=allow protocol=TCP localport=%d,%d profile=any",
                port, websocketPort
            );

            // The script combines both commands
            String script = String.format(
                "$p = Start-Process cmd.exe -ArgumentList '/c %s & %s' -Verb RunAs -Wait -PassThru -WindowStyle Hidden; exit $p.ExitCode",
                deleteCommand, addCommand
            );
            
            String[] command = {"powershell.exe", "-NoProfile", "-Command", script};
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Firewall optimization completed via UAC for ports {} and {}", port, websocketPort);
                journalConsumer.accept(
                        "Брандмауер успішно налаштовано для портів " + port + " і " + websocketPort + ".",
                        "INFO"
                );
            } else {
                logger.error("Firewall optimization failed via UAC with exit code {}", exitCode);
                journalConsumer.accept(
                        "Не вдалося налаштувати брандмауер для портів " + port + " і " + websocketPort
                                + " (код помилки: " + exitCode + ").",
                        "ERROR"
                );
            }
        } catch (Exception e) {
            logger.error("Failed to trigger firewall optimization", e);
            journalConsumer.accept(
                    "Не вдалося запустити налаштування брандмауера: " + e.getMessage(),
                    "ERROR"
            );
        }
    }

    /**
     * Checks if a firewall rule for the given port exists and is enabled.
     */
    public boolean isPortAllowedInFirewall(int port) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return true;

        try {
            int websocketPort = port + 2;
            // netsh might return localized output, so we check for common success markers
            // and verify that both dashboard ports are mentioned in the rule.
            String output = executeCommandWithOutput("netsh advfirewall firewall show rule name=\"SchoolBell Web Dashboard\"");
            
            if (output == null || output.isEmpty()) return false;

            boolean isEnabled = output.contains("Yes") || output.contains("Так") || output.contains("Да");
            boolean isPortMatch = output.contains(String.valueOf(port))
                    && output.contains(String.valueOf(websocketPort));
            
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
            if (file.getName().toLowerCase().endsWith(".jar")) {
                File parent = file.getParentFile();
                if (parent != null && parent.getName().equalsIgnoreCase("app")) {
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
            int clampedLevel = Math.clamp(level, 0, 100);
            double volumeScalar = clampedLevel / 100.0;
            
            String script = 
                "$definition = @'\n" +
                "using System;\n" +
                "using System.Runtime.InteropServices;\n" +
                "\n" +
                "[Guid(\"5CDF2C82-841E-4546-9722-0CF74078229A\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\n" +
                "interface IAudioEndpointVolume {\n" +
                "    int f(); int g(); int h(); int i();\n" +
                "    int SetMasterVolumeLevelScalar(float fLevel, Guid pguidEventContext);\n" +
                "    int j();\n" +
                "    int GetMasterVolumeLevelScalar(out float pfLevel);\n" +
                "    int k(); int l(); int m(); int n();\n" +
                "    int SetMute(bool bMute, Guid pguidEventContext);\n" +
                "    int GetMute(out bool pbMute);\n" +
                "}\n" +
                "\n" +
                "[Guid(\"D666063F-1587-4E43-81F1-B948E807363F\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\n" +
                "interface IMMDevice {\n" +
                "    int Activate(ref Guid id, int clsCtx, int activationParams, out IAudioEndpointVolume aev);\n" +
                "}\n" +
                "\n" +
                "[Guid(\"A95664D2-9614-4F35-A746-DE8DB63617E6\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\n" +
                "interface IMMDeviceEnumerator {\n" +
                "    int f();\n" +
                "    int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice endpoint);\n" +
                "}\n" +
                "\n" +
                "[ComImport, Guid(\"BCDE0395-E52F-467C-8E3D-C4579291692E\")]\n" +
                "class MMDeviceEnumeratorComObject { }\n" +
                "\n" +
                "public class Audio {\n" +
                "    public static void SetVolume(float level) {\n" +
                "        var enumerator = new MMDeviceEnumeratorComObject() as IMMDeviceEnumerator;\n" +
                "        IMMDevice dev;\n" +
                "        enumerator.GetDefaultAudioEndpoint(0, 1, out dev);\n" +
                "        IAudioEndpointVolume epv;\n" +
                "        var epvid = typeof(IAudioEndpointVolume).GUID;\n" +
                "        dev.Activate(ref epvid, 23, 0, out epv);\n" +
                "        epv.SetMasterVolumeLevelScalar(level, Guid.Empty);\n" +
                "        if (level > 0.0f) {\n" +
                "            epv.SetMute(false, Guid.Empty);\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "'@\n" +
                "Add-Type -TypeDefinition $definition\n" +
                "[Audio]::SetVolume(" + volumeScalar + ")";

            byte[] utf16Bytes = script.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
            String base64Script = java.util.Base64.getEncoder().encodeToString(utf16Bytes);
            
            String[] command = {"powershell.exe", "-NoProfile", "-EncodedCommand", base64Script};
            Runtime.getRuntime().exec(command);
            logger.info("System volume set to {}%", clampedLevel);
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
                File dbFile = new File(PathService.getDatabasePath());
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

    /**
     * Gathers and logs system diagnostic information.
     */
    public void logSystemDiagnostics(com.schoolbell.SystemJournal journal) {
        String os = System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") v" + System.getProperty("os.version");
        String javaVer = System.getProperty("java.version");
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / 1024 / 1024;
        long totalMem = runtime.totalMemory() / 1024 / 1024;
        String hostInfo;
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            hostInfo = localHost.getHostName() + " (" + localHost.getHostAddress() + ")";
        } catch (Exception e) {
            hostInfo = "невизначено";
        }
        String relayInfo = config.getRelayType() + (config.getShellyIp().isEmpty() ? "" : " [" + config.getShellyIp() + "]");

        logger.info("SYSTEM DIAGNOSTICS: OS={}, Java={}, Memory={}/{}MB, Host={}, Relay={}, Port={}",
                os, javaVer, totalMem, maxMem, hostInfo, relayInfo, config.getBroadcastPort());

        journal.addLog("Ініціалізація системи: " + os + ", Java " + javaVer + ", Пам'ять " + totalMem + "/" + maxMem + " MB", "INFO");
        journal.addLog("Апаратна конфігурація: Реле " + relayInfo + ", Порт " + config.getBroadcastPort(), "INFO");
    }

    /**
     * Exports all system logs from the database to a text file.
     */
    public void exportLogs(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Експортувати системний журнал");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        fileChooser.setInitialFileName("school_bell_logs_" + timestamp + ".txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстові файли", "*.txt"));

        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            try {
                java.util.List<String> logs = DatabaseManager.getSystemLogs(7);
                // Database returns logs in DESC order (newest first), usually for export we want ASC or just as is.
                // Let's reverse them for a chronological report.
                java.util.Collections.reverse(logs);
                
                StringBuilder content = new StringBuilder();
                content.append("--- SchoolBell System Log Export ---\n");
                DateTimeFormatter uaFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                content.append("Exported at: ").append(LocalDateTime.now().format(uaFormatter)).append("\n\n");
                
                for (String log : logs) {
                    content.append(log).append("\n");
                }
                
                Files.writeString(file.toPath(), content.toString());
                logger.info("Logs exported successfully to: " + file.getAbsolutePath());
                com.schoolbell.ui.ToastService.showSuccess("Журнал успішно експортовано!");
            } catch (IOException e) {
                logger.error("Failed to export logs", e);
                com.schoolbell.ui.ToastService.showError("Помилка при експорті журналів");
            }
        }
    }
}
