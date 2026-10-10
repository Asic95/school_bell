package com.schoolbell.updater;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Small, JavaFX-free update process.
 *
 * Phase 3 responsibilities:
 * - run independently from SchoolBell;
 * - install a verified installer passed by the main application;
 * - optionally check the manifest and download an installer when scheduled;
 * - wait for the main process to exit before starting Inno Setup;
 * - keep a bounded technical log and mirror important events to the system journal.
 */
public final class SchoolBellUpdater {
    private static final String DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/Asic95/school_bell/master/updates.json";
    private static final String MANIFEST_URL = System.getProperty(
            "schoolbell.update.manifest.url", DEFAULT_MANIFEST_URL);
    private static final String APP_NAME = "SchoolBell";
    private static final String MAIN_EXE = "SchoolBell.exe";
    private static final String UPDATER_EXE = "SchoolBellUpdater.exe";
    private static final long MAX_LOG_BYTES = 1024 * 1024;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(15);
    private static final Duration PROCESS_WAIT_TIMEOUT = Duration.ofMinutes(30);
    private static final DateTimeFormatter LOG_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path appData;
    private final Path updaterDirectory;
    private final Path logFile;
    private final Path lockFile;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Gson gson = new Gson();

    private SchoolBellUpdater() {
        String appDataValue = System.getenv("APPDATA");
        if (appDataValue == null || appDataValue.isBlank()) {
            appDataValue = System.getProperty("user.home", ".");
        }
        appData = Path.of(appDataValue, APP_NAME);
        updaterDirectory = appData.resolve("updater");
        logFile = updaterDirectory.resolve("updater.log");
        lockFile = updaterDirectory.resolve("updater.lock");
    }

    public static void main(String[] args) {
        SchoolBellUpdater updater = new SchoolBellUpdater();
        try {
            updater.run(args);
        } catch (Exception e) {
            updater.log("ERROR", "Updater завершився з помилкою: " + safeMessage(e));
            System.exit(1);
        }
    }

    private void run(String[] args) throws Exception {
        Files.createDirectories(updaterDirectory);
        rotateLogIfNeeded();

        try (FileChannel channel = FileChannel.open(lockFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.tryLock()) {
            if (ignored == null) {
                log("INFO", "Інший updater уже працює. Завершення поточного запуску.");
                return;
            }

            Arguments arguments = Arguments.parse(args);
            log("INFO", "Updater запущено. Режим: " + (arguments.scheduled ? "scheduled" : "install"));

            if (arguments.installer != null) {
                installPassedInstaller(arguments);
            } else if (arguments.scheduled) {
                checkAndDownloadOnly();
            } else {
                log("WARN", "Не передано інсталятор і не вказано scheduled-режим.");
            }
        }
    }

    private void installPassedInstaller(Arguments arguments) throws Exception {
        Path installer = arguments.installer.toAbsolutePath().normalize();
        if (!Files.isRegularFile(installer)) {
            throw new IOException("Інсталятор не знайдено: " + installer);
        }

        if (arguments.waitPid > 0) {
            waitForProcessToExit(arguments.waitPid);
        }
        waitForSchoolBellProcessToExit();

        log("INFO", "Запуск інсталятора: " + installer);
        Process installerProcess = new ProcessBuilder(
                installer.toString(),
                "/VERYSILENT",
                "/CLOSEAPPLICATIONS",
                "/NORESTART"
        ).directory(installer.getParent().toFile()).start();

        int exitCode = installerProcess.waitFor();
        log("INFO", "Інсталятор завершився з кодом: " + exitCode);
        if (exitCode != 0) {
            throw new IOException("Інсталятор завершився з кодом " + exitCode);
        }

        Path mainExecutable = resolveMainExecutable();
        if (mainExecutable != null && Files.isRegularFile(mainExecutable)) {
            log("INFO", "Запуск оновленої SchoolBell: " + mainExecutable);
            new ProcessBuilder(mainExecutable.toString()).directory(mainExecutable.getParent().toFile()).start();
        } else {
            log("WARN", "Не вдалося автоматично знайти SchoolBell.exe після оновлення.");
        }
        logToSystemJournal("Оновлення SchoolBell успішно встановлено.", "INFO");
    }

    /**
     * Scheduled mode intentionally downloads only. Automatic installation after
     * the policy deadline belongs to phase 4, after the safe-state handshake exists.
     */
    private void checkAndDownloadOnly() throws Exception {
        Manifest manifest = fetchManifest();
        if (manifest == null || manifest.latestVersion == null || manifest.downloadUrl == null) {
            log("INFO", "Manifest не містить придатного оновлення.");
            return;
        }

        String currentVersion = readCurrentVersion();
        if (!isNewerVersion(manifest.latestVersion, currentVersion)) {
            log("INFO", "Встановлена версія " + currentVersion + " актуальна.");
            return;
        }

        Path downloadDirectory = updaterDirectory.resolve("downloads");
        Files.createDirectories(downloadDirectory);
        Path target = downloadDirectory.resolve("SchoolBell_Setup_v" + sanitizeVersion(manifest.latestVersion) + ".exe");
        if (Files.isRegularFile(target) && manifest.checksum != null &&
                manifest.checksum.equalsIgnoreCase(sha256(target))) {
            log("INFO", "Оновлення " + manifest.latestVersion + " уже завантажене та перевірене.");
            return;
        }

        log("INFO", "Початок автономного завантаження оновлення " + manifest.latestVersion + ".");
        Path temporary = target.resolveSibling(target.getFileName() + ".part");
        download(manifest.downloadUrl, temporary);
        if (manifest.checksum != null && !manifest.checksum.isBlank() &&
                !manifest.checksum.equalsIgnoreCase(sha256(temporary))) {
            Files.deleteIfExists(temporary);
            throw new IOException("Перевірка SHA-256 завантаженого інсталятора не пройшла.");
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        log("INFO", "Оновлення " + manifest.latestVersion + " завантажене та перевірене.");
        logToSystemJournal("Updater завантажив оновлення " + manifest.latestVersion + ".", "INFO");
    }

    private Manifest fetchManifest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MANIFEST_URL))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Manifest повернув HTTP " + response.statusCode());
        }
        return gson.fromJson(response.body(), Manifest.class);
    }

    private void download(String url, Path target) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DOWNLOAD_TIMEOUT)
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Інсталятор повернув HTTP " + response.statusCode());
        }

        try (InputStream input = new BufferedInputStream(response.body());
             OutputStream output = Files.newOutputStream(target,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            input.transferTo(output);
        }
    }

    private void waitForProcessToExit(long pid) throws InterruptedException, IOException {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty()) return;
        log("INFO", "Очікування завершення процесу SchoolBell, PID=" + pid + ".");
        waitForHandle(handle.get());
    }

    private void waitForSchoolBellProcessToExit() throws InterruptedException, IOException {
        Instant deadline = Instant.now().plus(PROCESS_WAIT_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            boolean running = ProcessHandle.allProcesses().anyMatch(this::isSchoolBellProcess);
            if (!running) return;
            Thread.sleep(500);
        }
        throw new IOException("SchoolBell не завершилася протягом дозволеного часу.");
    }

    private void waitForHandle(ProcessHandle handle) throws InterruptedException, IOException {
        Instant deadline = Instant.now().plus(PROCESS_WAIT_TIMEOUT);
        while (handle.isAlive() && Instant.now().isBefore(deadline)) {
            Thread.sleep(500);
        }
        if (handle.isAlive()) {
            throw new IOException("Процес SchoolBell не завершився протягом дозволеного часу.");
        }
    }

    private boolean isSchoolBellProcess(ProcessHandle handle) {
        String command = handle.info().command().orElse("");
        return command.toLowerCase(Locale.ROOT).endsWith("\\" + MAIN_EXE.toLowerCase(Locale.ROOT))
                || command.equalsIgnoreCase(MAIN_EXE);
    }

    private Path resolveMainExecutable() {
        String updaterCommand = ProcessHandle.current().info().command().orElse(null);
        if (updaterCommand != null) {
            Path sibling = Path.of(updaterCommand).toAbsolutePath().getParent().resolve(MAIN_EXE);
            if (Files.isRegularFile(sibling)) return sibling;
        }

        try {
            Path codeLocation = Path.of(SchoolBellUpdater.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path appDirectory = Files.isDirectory(codeLocation) ? codeLocation : codeLocation.getParent();
            if (appDirectory != null && "app".equalsIgnoreCase(String.valueOf(appDirectory.getFileName()))) {
                Path sibling = appDirectory.getParent().resolve(MAIN_EXE);
                if (Files.isRegularFile(sibling)) return sibling;
            }
        } catch (Exception ignored) {
            // Fall through to the working-directory lookup below.
        }

        Path currentDirectory = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath();
        Path sibling = currentDirectory.resolve(MAIN_EXE);
        return Files.isRegularFile(sibling) ? sibling : null;
    }

    private String readCurrentVersion() {
        return System.getProperty("schoolbell.version", "0.0.0");
    }

    private static boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int l = i < latestParts.length ? numberPart(latestParts[i]) : 0;
            int c = i < currentParts.length ? numberPart(currentParts[i]) : 0;
            if (l != c) return l > c;
        }
        return false;
    }

    private static int numberPart(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String sanitizeVersion(String version) {
        return version.replaceAll("[^0-9A-Za-z._-]", "_");
    }

    private static String sha256(Path file) throws Exception {
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format("%02x", value));
        return result.toString();
    }

    private void log(String level, String message) {
        try {
            Files.createDirectories(updaterDirectory);
            rotateLogIfNeeded();
            try (BufferedWriter writer = Files.newBufferedWriter(logFile,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write("[" + LocalDateTime.now().format(LOG_TIME) + "] [" + level + "] " + message);
                writer.newLine();
            }
        } catch (IOException ignored) {
            // Logging must never prevent an update attempt.
        }
    }

    private void rotateLogIfNeeded() throws IOException {
        if (!Files.isRegularFile(logFile) || Files.size(logFile) <= MAX_LOG_BYTES) return;
        Path previous = updaterDirectory.resolve("updater.log.1");
        Files.deleteIfExists(previous);
        Files.move(logFile, previous, StandardCopyOption.REPLACE_EXISTING);
    }

    private void logToSystemJournal(String message, String level) {
        Path database = appData.resolve("school_bell.db");
        if (!Files.isRegularFile(database)) return;
        String url = "jdbc:sqlite:" + database.toAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO system_logs (level, message, timestamp) VALUES (?, ?, datetime('now', 'localtime'))")) {
            statement.setString(1, level);
            statement.setString(2, message);
            statement.executeUpdate();
        } catch (Exception e) {
            log("WARN", "Не вдалося записати подію в системний журнал: " + safeMessage(e));
        }
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private record Manifest(
            @SerializedName("latest_version") String latestVersion,
            @SerializedName("download_url") String downloadUrl,
            String checksum
    ) {}

    private static final class Arguments {
        private Path installer;
        private long waitPid;
        private boolean scheduled;

        private static Arguments parse(String[] args) {
            Arguments result = new Arguments();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--installer" -> {
                        if (i + 1 < args.length) result.installer = Path.of(args[++i]);
                    }
                    case "--wait-pid" -> {
                        if (i + 1 < args.length) {
                            try { result.waitPid = Long.parseLong(args[++i]); }
                            catch (NumberFormatException ignored) { result.waitPid = 0; }
                        }
                    }
                    case "--scheduled" -> result.scheduled = true;
                    default -> { }
                }
            }
            return result;
        }
    }
}
