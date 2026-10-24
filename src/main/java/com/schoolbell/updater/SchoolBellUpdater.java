package com.schoolbell.updater;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.schoolbell.service.PathService;
import com.schoolbell.service.UpdatePolicyState;
import com.schoolbell.service.UpdateSafetyState;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

    private final Path updaterDirectory;
    private final Path logFile;
    private final Path lockFile;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Gson gson = new Gson();

    private SchoolBellUpdater() {
        updaterDirectory = PathService.getSharedUpdaterDirectory();
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

            // Periodic cleanup of stale .part files, obsolete installers and rotated logs
            UpdaterStorageCleaner.cleanStorage(arguments.installer, msg -> log("INFO", msg));

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
            try {
                waitForProcessToExit(arguments.waitPid);
            } catch (IOException e) {
                log("WARN", "SchoolBell не завершилася штатно: " + safeMessage(e) + ". Спроба примусового завершення.");
                terminateRemainingSchoolBellProcesses();
            }
        }
        terminateRemainingSchoolBellProcesses();
        waitForSchoolBellProcessToExit();

        log("INFO", "Запуск інсталятора: " + installer);
        Path installerLog = updaterDirectory.resolve("installer.log");
        log("INFO", "Параметри інсталятора: /VERYSILENT /SUPPRESSMSGBOXES /FORCECLOSEAPPLICATIONS /NORESTART /LOG=\""
                + installerLog + "\"");
        Process installerProcess = new ProcessBuilder(
                installer.toString(),
                "/VERYSILENT",
                "/SUPPRESSMSGBOXES",
                "/FORCECLOSEAPPLICATIONS",
                "/NORESTART",
                "/LOG=" + installerLog.toAbsolutePath()
        ).directory(installer.getParent().toFile()).start();

        log("INFO", "Процес інсталятора запущено, PID=" + installerProcess.pid() + ".");
        int exitCode = installerProcess.waitFor();
        log("INFO", "Інсталятор завершився з кодом: " + exitCode);
        if (exitCode != 0) {
            throw new IOException("Інсталятор завершився з кодом " + exitCode);
        }

        // Do NOT launch SchoolBell.exe when running as SYSTEM (Session 0).
        // GUI applications cannot display windows in the non-interactive session.
        // The user will see the updated version the next time they open SchoolBell normally.
        if (isNonInteractiveSession()) {
            log("INFO", "Updater працює в неінтерактивній сесії (SYSTEM). "
                    + "SchoolBell.exe не запускається — користувач побачить оновлену версію при наступному відкритті.");
        } else {
            Path mainExecutable = resolveMainExecutable(arguments);
            if (mainExecutable != null && Files.isRegularFile(mainExecutable)) {
                log("INFO", "Запуск оновленої SchoolBell: " + mainExecutable);
                new ProcessBuilder(mainExecutable.toString()).directory(mainExecutable.getParent().toFile()).start();
            } else {
                log("WARN", "Не вдалося автоматично знайти SchoolBell.exe після оновлення.");
            }
        }
        if (arguments.selfDelete != null) {
            scheduleSelfDelete(arguments.selfDelete);
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

        Path downloadDirectory = PathService.getSharedUpdaterDownloadsDirectory();
        Files.createDirectories(downloadDirectory);
        Path target = downloadDirectory.resolve("SchoolBell_Setup_v" + sanitizeVersion(manifest.latestVersion) + ".exe");
        if (Files.isRegularFile(target) && manifest.checksum != null &&
                manifest.checksum.equalsIgnoreCase(sha256(target))) {
            maybeInstallAfterDeadline(manifest, target);
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
        UpdaterStorageCleaner.cleanStorage(target, msg -> log("INFO", msg));
        maybeInstallAfterDeadline(manifest, target);
    }

    private void maybeInstallAfterDeadline(Manifest manifest, Path target) throws Exception {
        if (manifest == null || manifest.critical || !shouldInstallAfterDeadline(manifest.latestVersion)) return;
        if (!isSafeToInstall()) {
            log("INFO", "П'ятиденний дедлайн минув, але безпечного моменту немає. Спроба буде повторена пізніше.");
            return;
        }

        log("INFO", "П'ятиденний дедлайн минув, безпечний момент підтверджено. Починаємо автоматичне встановлення.");
        UpdateSafetyState.State safety = UpdateSafetyState.read();
        if (safety != null && safety.autostart()) {
            Path flag = updaterDirectory.resolve("restart.flag");
            try {
                Files.writeString(flag, "1", StandardCharsets.UTF_8);
                log("INFO", "Створено restart.flag: у програмі увімкнено автозапуск, після оновлення вона перезапуститься.");
            } catch (IOException e) {
                log("WARN", "Не вдалося створити restart.flag: " + e.getMessage());
            }
        }
        terminateRemainingSchoolBellProcesses();
        // Remove the Windows autostart registry entry so SchoolBell cannot be relaunched
        // by Windows while the installer is replacing its files. The installer will re-create
        // the entry during setup (via CurStepChanged / [Registry] section).
        disableAutostartRegistry();
        waitForSchoolBellProcessToExit();
        launchInstallerDetached(target);
        log("INFO", "Автоматичний updater завершився після запуску інсталятора. Завершення установки записується в installer.log.");
    }

    /**
     * Removes the SchoolBell autostart entry from HKCU Run registry key.
     * This prevents SchoolBell from being relaunched by Windows while the installer
     * is replacing the application files. The installer re-creates the entry.
     */
    private void disableAutostartRegistry() {
        try {
            Process reg = new ProcessBuilder(
                    "reg", "delete",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", "SchoolBell",
                    "/f"
            ).redirectErrorStream(true).start();
            String out = new String(reg.getInputStream().readAllBytes()).trim();
            int code = reg.waitFor();
            if (code == 0) {
                log("INFO", "Запис автозапуску SchoolBell тимчасово видалено з реєстру перед оновленням.");
            } else {
                log("INFO", "Запис автозапуску не знайдено в реєстрі (або вже відсутній): " + out);
            }
        } catch (Exception e) {
            log("WARN", "Не вдалося видалити запис автозапуску з реєстру: " + e.getMessage());
        }
    }

    /**
     * The scheduled updater is itself installed in the directory that Inno Setup
     * replaces. Let the updater exit first so the installer can replace its files.
     */
    private void launchInstallerDetached(Path installer) throws IOException {
        Path installerLog = updaterDirectory.resolve("installer.log");
        Process process = new ProcessBuilder(
                installer.toAbsolutePath().toString(),
                "/VERYSILENT",
                "/SUPPRESSMSGBOXES",
                "/FORCECLOSEAPPLICATIONS",
                "/NORESTART",
                "/LOG=" + installerLog.toAbsolutePath()
        ).directory(installer.getParent().toFile())
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        log("INFO", "Від’єднаний процес інсталятора запущено, PID=" + process.pid()
                + ". Updater завершується для заміни своїх файлів.");
    }

    private boolean shouldInstallAfterDeadline(String version) {
        long now = System.currentTimeMillis();
        UpdatePolicyState.State state = UpdatePolicyState.read();
        if (state == null || version == null || !version.equalsIgnoreCase(state.version())) {
            state = UpdatePolicyState.observe(version, now, 5L * 24 * 60 * 60 * 1000);
        }
        if (now >= state.deadline_at() && !state.install_pending()) {
            state = UpdatePolicyState.markPending(version, now, 5L * 24 * 60 * 60 * 1000);
        }
        return state.install_pending() || now >= state.deadline_at();
    }

    private boolean isSafeToInstall() {
        boolean schoolBellRunning = ProcessHandle.allProcesses().anyMatch(this::isSchoolBellProcess);
        if (!schoolBellRunning) return true;
        UpdateSafetyState.State safety = UpdateSafetyState.read();
        if (safety == null) return false;
        long age = System.currentTimeMillis() - safety.timestamp();
        return safety.safe() && age >= 0 && age <= 30_000;
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

    private void terminateRemainingSchoolBellProcesses() throws InterruptedException {
        Process taskKill = null;
        try {
            taskKill = new ProcessBuilder("taskkill", "/F", "/T", "/IM", MAIN_EXE)
                    .redirectErrorStream(true)
                    .start();
            // taskkill.exe writes localized console text using the Windows OEM code page,
            // not the JVM's UTF-8/default charset. Decode it explicitly to keep updater.log readable.
            String output = new String(taskKill.getInputStream().readAllBytes(), Charset.forName("IBM866")).trim();
            int exitCode = taskKill.waitFor();
            if (exitCode == 0) {
                log("INFO", "Залишковий процес SchoolBell завершено перед встановленням оновлення.");
                logToSystemJournal("Перед встановленням оновлення було примусово завершено залишковий процес SchoolBell.", "WARNING");
            } else if (!output.isBlank()) {
                log("INFO", "Активних процесів SchoolBell для завершення не знайдено: " + output);
            }
        } catch (IOException e) {
            log("WARN", "Не вдалося виконати примусове завершення SchoolBell: " + safeMessage(e));
        } finally {
            if (taskKill != null && taskKill.isAlive()) {
                taskKill.destroyForcibly();
            }
        }
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

    private Path resolveMainExecutable(Arguments arguments) {
        if (arguments.mainExecutable != null && Files.isRegularFile(arguments.mainExecutable)) {
            return arguments.mainExecutable.toAbsolutePath().normalize();
        }

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

    private void scheduleSelfDelete(Path selfDelete) {
        try {
            String command = "ping 127.0.0.1 -n 3 > nul & del /f /q \""
                    + selfDelete.toAbsolutePath() + "\"";
            new ProcessBuilder("cmd.exe", "/c", command).start();
        } catch (IOException e) {
            log("WARN", "Не вдалося запланувати очищення тимчасового updater-файлу: " + safeMessage(e));
        }
    }
    /**
     * Detects whether the updater is running in the non-interactive Session 0
     * (e.g. as a SYSTEM scheduled task). GUI applications cannot display windows
     * in Session 0, so we must not attempt to launch SchoolBell.exe there.
     *
     * Detection: on Windows, {@code query process} or the {@code SessionName}
     * environment variable is unreliable from SYSTEM tasks. Instead we check
     * whether the current user name is literally {@code "SYSTEM"} (or the
     * localized equivalent) — scheduled tasks in the .iss are created with
     * {@code /RU SYSTEM}.
     */
    private static boolean isNonInteractiveSession() {
        String userName = System.getProperty("user.name", "");
        if (userName.equalsIgnoreCase("SYSTEM") || userName.equalsIgnoreCase("СИСТЕМА")) {
            return true;
        }
        // Fallback: check the Windows session name environment variable.
        // In an interactive logon session it is typically "Console" or an RDP name;
        // in Session 0 it is usually "Services" or absent.
        String sessionName = System.getenv("SESSIONNAME");
        if (sessionName != null && sessionName.equalsIgnoreCase("Services")) {
            return true;
        }
        return false;
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
        Path eventsDirectory = PathService.getSharedUpdaterEventsDirectory();
        try {
            Files.createDirectories(eventsDirectory);
            Path temporary = eventsDirectory.resolve(UUID.randomUUID() + ".event.part");
            Path event = eventsDirectory.resolve(temporary.getFileName().toString().replace(".part", ""));
            Files.writeString(temporary, level + System.lineSeparator() + message + System.lineSeparator(),
                    java.nio.charset.StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            Files.move(temporary, event, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            log("WARN", "Не вдалося передати подію в системний журнал: " + safeMessage(e));
        }
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private record Manifest(
            @SerializedName("latest_version") String latestVersion,
            @SerializedName("download_url") String downloadUrl,
            String checksum,
            boolean critical
    ) {}

    private static final class Arguments {
        private Path installer;
        private Path mainExecutable;
        private Path selfDelete;
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
                    case "--main-exe" -> {
                        if (i + 1 < args.length) result.mainExecutable = Path.of(args[++i]);
                    }
                    case "--self-delete" -> {
                        if (i + 1 < args.length) result.selfDelete = Path.of(args[++i]);
                    }
                    case "--scheduled" -> result.scheduled = true;
                    default -> { }
                }
            }
            return result;
        }
    }
}
