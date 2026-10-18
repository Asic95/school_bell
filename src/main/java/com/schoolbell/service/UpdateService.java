package com.schoolbell.service;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.schoolbell.MainApp;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class UpdateService {
    private static final Logger logger = LoggerFactory.getLogger(UpdateService.class);
    public static final long UPDATE_INSTALL_DELAY_MS = 5L * 24 * 60 * 60 * 1000;
    public static final String SETTING_FIRST_SEEN_AT = "update.first_seen_at";
    public static final String SETTING_DEADLINE_AT = "update.deadline_at";
    public static final String SETTING_INSTALL_PENDING = "update.install_pending";
    private static final String DEFAULT_MANIFEST_URL = "https://raw.githubusercontent.com/Asic95/school_bell/master/updates.json";
    private static final String MANIFEST_URL = System.getProperty(
            "schoolbell.update.manifest.url", DEFAULT_MANIFEST_URL);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Gson gson = new Gson();
    private BiConsumer<String, String> journalConsumer;

    public enum DownloadStatus {
        IDLE,
        DOWNLOADING,
        VERIFIED,
        FAILED
    }

    private volatile DownloadStatus downloadStatus = DownloadStatus.IDLE;
    private volatile double downloadProgress = 0.0;
    private volatile File verifiedInstallerFile = null;
    private volatile String downloadingVersion = null;
    private volatile CompletableFuture<File> activeDownloadFuture = null;

    private final List<Consumer<DownloadStatus>> statusListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Double>> progressListeners = new CopyOnWriteArrayList<>();

    public void setJournalConsumer(BiConsumer<String, String> consumer) {
        this.journalConsumer = consumer;
    }

    private void logToJournal(String message) {
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        String level = normalized.contains("помил")
                || normalized.contains("не вдалося")
                || normalized.contains("не знайдено")
                || normalized.contains("не вдалося")
                ? "ERROR" : "INFO";
        logToJournal(message, level);
    }

    private void logToJournal(String message, String level) {
        switch (level) {
            case "ERROR" -> logger.error(message);
            case "WARNING" -> logger.warn(message);
            default -> logger.info(message);
        }
        if (journalConsumer != null) {
            journalConsumer.accept(message, level);
        }
    }

    /**
     * Scans the system temp directory for leftover installer files from previous updates
     * (schoolbell_update_*.exe) and deletes them. Called once on startup.
     */
    public void cleanupLeftoverInstallers() {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null) return;

        File dir = new File(tempDir);
        File[] leftovers = dir.listFiles(
            (d, name) -> (name.startsWith("schoolbell_update_") || name.startsWith("schoolbell_updater_"))
                    && name.endsWith(".exe")
        );
        if (leftovers == null || leftovers.length == 0) return;

        // Keep recent installers so an interrupted update can be resumed instead
        // of downloading the same large file again on the next launch.
        long cleanupCutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;

        for (File f : leftovers) {
            if (f.lastModified() > cleanupCutoff) {
                continue;
            }
            if (verifiedInstallerFile != null && f.getAbsolutePath().equalsIgnoreCase(verifiedInstallerFile.getAbsolutePath())) {
                continue;
            }
            try {
                if (f.delete()) {
                    logToJournal("Очистка: видалено залишковий інсталятор оновлення: " + f.getName());
                } else {
                    logger.warn("Очистка: не вдалося видалити залишковий інсталятор: {}", f.getName());
                }
            } catch (Exception e) {
                logger.warn("Очистка: помилка при видаленні інсталятора '{}': {}", f.getName(), e.getMessage());
            }
        }

        // Clean shared updater storage (%ProgramData%/SchoolBell/updater)
        Path activeToKeep = verifiedInstallerFile != null ? verifiedInstallerFile.toPath() : null;
        com.schoolbell.updater.UpdaterStorageCleaner.cleanStorage(activeToKeep, msg -> logToJournal(msg, "INFO"));
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public double getDownloadProgress() {
        return downloadProgress;
    }

    public File getVerifiedInstallerFile() {
        return verifiedInstallerFile;
    }

    public String getDownloadingVersion() {
        return downloadingVersion;
    }

    public void addStatusListener(Consumer<DownloadStatus> listener) {
        statusListeners.add(listener);
        listener.accept(downloadStatus);
    }

    public void removeStatusListener(Consumer<DownloadStatus> listener) {
        statusListeners.remove(listener);
    }

    public void addProgressListener(Consumer<Double> listener) {
        progressListeners.add(listener);
        listener.accept(downloadProgress);
    }

    public void removeProgressListener(Consumer<Double> listener) {
        progressListeners.remove(listener);
    }

    private void notifyStatus(DownloadStatus status) {
        this.downloadStatus = status;
        for (Consumer<DownloadStatus> listener : statusListeners) {
            try {
                listener.accept(status);
            } catch (Exception e) {
                logger.error("Error in status listener", e);
            }
        }
    }

    private void notifyProgress(double progress) {
        this.downloadProgress = progress;
        for (Consumer<Double> listener : progressListeners) {
            try {
                listener.accept(progress);
            } catch (Exception e) {
                logger.error("Error in progress listener", e);
            }
        }
    }

    public synchronized CompletableFuture<File> startBackgroundDownload(UpdateManifest manifest) {
        if (manifest == null || manifest.downloadUrl() == null || manifest.downloadUrl().isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Manifest or download URL is missing"));
        }

        // If already downloaded and verified for this version
        if (downloadStatus == DownloadStatus.VERIFIED &&
                verifiedInstallerFile != null &&
                verifiedInstallerFile.exists() &&
                manifest.latestVersion().equalsIgnoreCase(downloadingVersion)) {
            logger.info("Update {} is already downloaded and verified at {}", downloadingVersion, verifiedInstallerFile.getAbsolutePath());
            return CompletableFuture.completedFuture(verifiedInstallerFile);
        }

        // If already actively downloading this version
        if (downloadStatus == DownloadStatus.DOWNLOADING &&
                activeDownloadFuture != null &&
                manifest.latestVersion().equalsIgnoreCase(downloadingVersion)) {
            return activeDownloadFuture;
        }

        this.downloadingVersion = manifest.latestVersion();
        this.verifiedInstallerFile = null;
        this.downloadProgress = 0.0;
        notifyStatus(DownloadStatus.DOWNLOADING);
        logToJournal("Початок фонового завантаження оновлення " + manifest.latestVersion() + "...");

        activeDownloadFuture = CompletableFuture
                .supplyAsync(() -> findExistingVerifiedInstaller(manifest))
                .thenCompose(existing -> existing != null
                        ? CompletableFuture.completedFuture(existing)
                        : downloadUpdate(manifest, this::notifyProgress))
                .thenApply(file -> {
                    this.verifiedInstallerFile = file;
                    this.downloadProgress = 1.0;
                    notifyStatus(DownloadStatus.VERIFIED);
                    logToJournal("Фонове завантаження завершено. Оновлення готове до встановлення!");
                    return file;
                }).exceptionally(ex -> {
                    this.verifiedInstallerFile = null;
                    notifyStatus(DownloadStatus.FAILED);
                    logToJournal("Помилка фонового завантаження оновлення: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    throw (ex instanceof RuntimeException re ? re : new RuntimeException(ex));
                });

        return activeDownloadFuture;
    }

    public static final String SETTING_SNOOZED_VERSION = "update.snoozed_version";
    public static final String SETTING_SNOOZED_UNTIL = "update.snoozed_until";
    public static final String SETTING_SNOOZE_COUNT = "update.snooze_count";

    public record UpdateManifest(
            @SerializedName("latest_version") String latestVersion,
            @SerializedName("release_date") String releaseDate,
            boolean critical,
            @SerializedName("minimum_supported_version") String minimumSupportedVersion,
            List<String> changelog,
            @SerializedName("download_url") String downloadUrl,
            String checksum
    ) {}

    public boolean isUpdateSnoozed(UpdateManifest manifest) {
        if (manifest == null || manifest.latestVersion() == null) return false;
        String snoozedVersion = DatabaseManager.getSetting(SETTING_SNOOZED_VERSION, "");
        if (!manifest.latestVersion().equalsIgnoreCase(snoozedVersion)) {
            return false;
        }
        String untilStr = DatabaseManager.getSetting(SETTING_SNOOZED_UNTIL, "0");
        /* if (Boolean.parseBoolean(System.getProperty("schoolbell.direct.update", "true"))) {
            try {
                ProcessBuilder directInstaller = new ProcessBuilder(
                        installerFile.getAbsolutePath(),
                        "/CLOSEAPPLICATIONS",
                        "/NORESTART"
                );
                directInstaller.directory(installerFile.getParentFile());
                directInstaller.start();
                clearAutomaticInstallState();
                logToJournal("Запущено перевірений інсталятор оновлення. SchoolBell завершує роботу.");
                Platform.exit();
                Thread shutdownFallback = new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    System.exit(0);
                }, "schoolbell-update-shutdown-fallback");
                shutdownFallback.setDaemon(true);
                shutdownFallback.start();
            } catch (IOException e) {
                logToJournal("Не вдалося запустити інсталятор оновлення: " + e.getMessage(), "ERROR");
            }
            return; */
        try {
            long until = Long.parseLong(untilStr);
            return System.currentTimeMillis() < until;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void snoozeUpdate(UpdateManifest manifest, long durationMillis) {
        if (manifest == null || manifest.latestVersion() == null) return;
        long actualDuration = durationMillis;
        if (manifest.critical()) {
            // Critical updates can only be postponed for up to 4 hours
            actualDuration = Math.min(durationMillis, 4 * 3600 * 1000L);
        }
        long until = System.currentTimeMillis() + actualDuration;
        int count = getSnoozeCount(manifest.latestVersion()) + 1;
        DatabaseManager.saveSetting(SETTING_SNOOZED_VERSION, manifest.latestVersion());
        DatabaseManager.saveSetting(SETTING_SNOOZED_UNTIL, String.valueOf(until));
        DatabaseManager.saveSetting(SETTING_SNOOZE_COUNT, String.valueOf(count));

        long hours = actualDuration / (3600 * 1000L);
        logToJournal(String.format("Оновлення %s відкладено на %d год. (всього відкладень: %d)",
                manifest.latestVersion(), hours, count));
    }

    public int getSnoozeCount(String version) {
        String snoozedVersion = DatabaseManager.getSetting(SETTING_SNOOZED_VERSION, "");
        if (version != null && version.equalsIgnoreCase(snoozedVersion)) {
            try {
                return Integer.parseInt(DatabaseManager.getSetting(SETTING_SNOOZE_COUNT, "0"));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    public void clearSnooze() {
        DatabaseManager.saveSetting(SETTING_SNOOZED_VERSION, "");
        DatabaseManager.saveSetting(SETTING_SNOOZED_UNTIL, "0");
        DatabaseManager.saveSetting(SETTING_SNOOZE_COUNT, "0");
    }

    public CompletableFuture<UpdateManifest> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logToJournal("Перевірка оновлень за адресою: " + MANIFEST_URL);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(MANIFEST_URL))
                        .timeout(java.time.Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    UpdateManifest manifest = gson.fromJson(response.body(), UpdateManifest.class);
                    String currentVersion = MainApp.VERSION;
                    String latestVersion = manifest.latestVersion();

                    logToJournal("Поточна версія: " + currentVersion + ", Доступна: " + latestVersion);

                    if (isNewerVersion(latestVersion)) {
                        trackUpdatePolicy(manifest);
                        logToJournal("Виявлено нову версію! Потрібне оновлення.");
                        return manifest;
                    } else {
                        logToJournal("У вас встановлена остання версія системи.");
                        return null;
                    }
                } else {
                    String err = "Помилка отримання маніфесту. Код сервера: " + response.statusCode();
                    logToJournal(err);
                    throw new IOException(err);
                }
            } catch (Exception e) {
                logToJournal("Помилка під час перевірки оновлень: " + e.getMessage());
                throw new java.util.concurrent.CompletionException(e);
            }
        });
    }

    private void trackUpdatePolicy(UpdateManifest manifest) {
        if (manifest == null || manifest.latestVersion() == null || manifest.critical()) return;

        long now = System.currentTimeMillis();
        UpdatePolicyState.State state = UpdatePolicyState.observe(
                manifest.latestVersion(), now, UPDATE_INSTALL_DELAY_MS);
        DatabaseManager.saveSetting(SETTING_FIRST_SEEN_AT, String.valueOf(state.first_seen_at()));
        DatabaseManager.saveSetting(SETTING_DEADLINE_AT, String.valueOf(state.deadline_at()));

        boolean pending = state.install_pending() || now >= state.deadline_at();
        if (pending && !state.install_pending()) {
            UpdatePolicyState.markPending(manifest.latestVersion(), now, UPDATE_INSTALL_DELAY_MS);
            logToJournal("Автоматичне встановлення після 5 днів очікує безпечного моменту.", "WARNING");
        }
        DatabaseManager.saveSetting(SETTING_INSTALL_PENDING, String.valueOf(pending));
    }

    public boolean isAutomaticInstallPending(UpdateManifest manifest) {
        if (manifest == null || manifest.latestVersion() == null || manifest.critical()) return false;
        UpdatePolicyState.State state = UpdatePolicyState.read();
        return state != null && manifest.latestVersion().equalsIgnoreCase(state.version())
                && (state.install_pending() || System.currentTimeMillis() >= state.deadline_at());
    }

    public boolean isNewerVersion(String latestVersion) {
        if (latestVersion == null) return false;
        String currentVersion = MainApp.VERSION;
        
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");
        
        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int current = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
            int latest = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            
            if (latest > current) return true;
            if (latest < current) return false;
        }
        return false;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private File findExistingVerifiedInstaller(UpdateManifest manifest) {
        if (manifest.checksum() == null || manifest.checksum().isBlank()) {
            return null;
        }

        File[] sharedCandidates = PathService.getSharedUpdaterDownloadsDirectory().toFile().listFiles(
                (dir, name) -> name.startsWith("SchoolBell_Setup_v") && name.endsWith(".exe"));
        File sharedVerified = findMatchingInstaller(sharedCandidates, manifest);
        if (sharedVerified != null) return sharedVerified;

        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null) return null;
        File[] candidates = new File(tempDir).listFiles(
                (dir, name) -> name.startsWith("schoolbell_update_") && name.endsWith(".exe"));
        if (candidates == null) return null;

        for (File candidate : candidates) {
            try {
                if (candidate.isFile() && manifest.checksum().equalsIgnoreCase(calculateChecksum(candidate.toPath()))) {
                    logToJournal("Знайдено раніше завантажене оновлення. Повторне завантаження не потрібне.");
                    return candidate;
                }
            } catch (Exception e) {
                logger.debug("Не вдалося перевірити залишковий інсталятор {}: {}", candidate.getName(), e.getMessage());
            }
        }
        return null;
    }

    private File findMatchingInstaller(File[] candidates, UpdateManifest manifest) {
        if (candidates == null) return null;
        for (File candidate : candidates) {
            try {
                if (candidate.isFile() && manifest.checksum().equalsIgnoreCase(calculateChecksum(candidate.toPath()))) {
                    logToJournal("Знайдено раніше завантажене оновлення. Повторне завантаження не потрібне.");
                    return candidate;
                }
            } catch (Exception e) {
                logger.debug("Не вдалося перевірити інсталятор {}: {}", candidate.getName(), e.getMessage());
            }
        }
        return null;
    }

    public CompletableFuture<File> downloadUpdate(UpdateManifest manifest, Consumer<Double> progressConsumer) {
        return CompletableFuture.supplyAsync(() -> {
            Path tempFile = null;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(manifest.downloadUrl()))
                        .timeout(java.time.Duration.ofMinutes(10))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new IOException("Server returned status code " + response.statusCode());
                }

                long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                Path downloadDirectory = PathService.getSharedUpdaterDownloadsDirectory();
                Files.createDirectories(downloadDirectory);
                Path target = downloadDirectory.resolve(
                        "SchoolBell_Setup_v" + sanitizeVersion(manifest.latestVersion()) + ".exe");
                tempFile = target.resolveSibling(target.getFileName() + ".part");
                
                try (InputStream is = response.body();
                     OutputStream os = Files.newOutputStream(tempFile)) {
                    
                    byte[] buffer = new byte[8192];
                    long downloadedBytes = 0;
                    int bytesRead;
                    int lastPercent = -1;
                    long lastLoggedMB = -1;
                    
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;
                        
                        if (totalBytes > 0) {
                            int percent = (int) ((double) downloadedBytes / totalBytes * 100);
                            if (percent > lastPercent) {
                                lastPercent = percent;
                                progressConsumer.accept((double) percent / 100);
                            }
                        } else {
                            long downloadedMB = downloadedBytes / (1024 * 1024);
                            if (downloadedMB > lastLoggedMB) {
                                lastLoggedMB = downloadedMB;
                                // Pass negative value to indicate unknown total size, value is MBs
                                progressConsumer.accept(-(double) downloadedBytes / (1024 * 1024));
                            }
                        }
                    }
                }

                if (manifest.checksum() != null && !manifest.checksum().isBlank()) {
                    String calculatedChecksum = calculateChecksum(tempFile);
                    if (!calculatedChecksum.equalsIgnoreCase(manifest.checksum())) {
                        Files.deleteIfExists(tempFile);
                        throw new IOException("Checksum verification failed");
                    }
                }

                Files.move(tempFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                return target.toFile();
            } catch (Exception e) {
                if (tempFile != null) {
                    try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
                }
                logger.error("Failed to download update: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private String calculateChecksum(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static String sanitizeVersion(String version) {
        return version == null ? "unknown" : version.replaceAll("[^0-9A-Za-z._-]", "_");
    }

    public void installUpdate(File installerFile) {
        if (Boolean.parseBoolean(System.getProperty("schoolbell.direct.update", "true"))) {
            try {
                ProcessBuilder directInstaller = new ProcessBuilder(
                        installerFile.getAbsolutePath(),
                        "/CLOSEAPPLICATIONS",
                        "/NORESTART"
                );
                directInstaller.directory(installerFile.getParentFile());
                directInstaller.start();
                logToJournal("Запущено перевірений інсталятор оновлення. SchoolBell завершує роботу.");
                Platform.exit();
                Thread shutdownFallback = new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    System.exit(0);
                }, "schoolbell-update-shutdown-fallback");
                shutdownFallback.setDaemon(true);
                shutdownFallback.start();
            } catch (IOException e) {
                logToJournal("Не вдалося запустити інсталятор оновлення: " + e.getMessage(), "ERROR");
            }
            return;
        }

        if (installerFile == null || !installerFile.isFile()) {
            logToJournal("Не вдалося запустити оновлення: перевірений інсталятор не знайдено.");
            return;
        }

        try {
            File updater = resolveUpdaterExecutable();
            if (updater == null || !updater.isFile()) {
                logToJournal("Не вдалося запустити оновлення: SchoolBellUpdater.exe не знайдено.");
                return;
            }

            File mainExecutable = resolveMainExecutable();
            Path appRoot = updater.toPath().toAbsolutePath().normalize().getParent();
            Path runtimeJava = Path.of(System.getProperty("java.home", ""), "bin", "javaw.exe");
            if (!Files.isRegularFile(runtimeJava)) {
                runtimeJava = appRoot.resolve("runtime").resolve("bin").resolve("javaw.exe");
            }
            Path appJar;
            try (java.util.stream.Stream<Path> appFiles = Files.list(appRoot.resolve("app"))) {
                appJar = appFiles
                        .filter(path -> path.getFileName().toString().startsWith("untitled-"))
                        .filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .findFirst()
                        .orElseThrow(() -> new IOException("SchoolBell JAR не знайдено"));
            }
            if (!Files.isRegularFile(runtimeJava)) {
                throw new IOException("Вбудований Java runtime не знайдено. Перевірено: " + runtimeJava);
            }

            java.util.List<String> updaterCommand = new java.util.ArrayList<>(java.util.List.of(
                    runtimeJava.toString(),
                    "-Dschoolbell.version=" + MainApp.VERSION,
                    "-Dschoolbell.update.manifest.url=" + System.getProperty(
                            "schoolbell.update.manifest.url", ""),
                    "-cp", appJar.toString(),
                    "com.schoolbell.updater.SchoolBellUpdater",
                    "--installer", installerFile.getAbsolutePath(),
                    "--wait-pid", String.valueOf(ProcessHandle.current().pid())
            ));
            if (mainExecutable != null && mainExecutable.isFile()) {
                updaterCommand.add("--main-exe");
                updaterCommand.add(mainExecutable.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(updaterCommand);
            pb.directory(appRoot.toFile());
            pb.start();
            clearAutomaticInstallState();
            logToJournal("Передано оновлення окремому updater-процесу. SchoolBell завершує роботу.");
            Platform.exit();
            Thread shutdownFallback = new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                System.exit(0);
            }, "schoolbell-update-shutdown-fallback");
            shutdownFallback.setDaemon(true);
            shutdownFallback.start();
        } catch (IOException e) {
            logToJournal("Не вдалося запустити SchoolBellUpdater.exe: " + e.getMessage());
        }
    }

    public void clearAutomaticInstallState() {
        UpdatePolicyState.clear();
        DatabaseManager.saveSetting(SETTING_FIRST_SEEN_AT, "0");
        DatabaseManager.saveSetting(SETTING_DEADLINE_AT, "0");
        DatabaseManager.saveSetting(SETTING_INSTALL_PENDING, "false");
    }

    private File resolveUpdaterExecutable() {
        String command = ProcessHandle.current().info().command().orElse(null);
        if (command != null) {
            File currentExecutable = new File(command);
            File sibling = new File(currentExecutable.getParentFile(), "SchoolBellUpdater.exe");
            if (sibling.isFile()) return sibling;
        }

        try {
            Path codeLocation = Path.of(UpdateService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path appDirectory = Files.isDirectory(codeLocation) ? codeLocation : codeLocation.getParent();
            if (appDirectory != null && "app".equalsIgnoreCase(String.valueOf(appDirectory.getFileName()))) {
                Path sibling = appDirectory.getParent().resolve("SchoolBellUpdater.exe");
                if (Files.isRegularFile(sibling)) return sibling.toFile();
            }
        } catch (URISyntaxException | RuntimeException ignored) {
            // Fall through to the working-directory lookup below.
        }

        File workingDirectorySibling = new File(System.getProperty("user.dir", "."), "SchoolBellUpdater.exe");
        if (workingDirectorySibling.isFile()) return workingDirectorySibling;

        return null;
    }

    private File resolveMainExecutable() {
        String command = ProcessHandle.current().info().command().orElse(null);
        if (command != null) {
            File currentExecutable = new File(command);
            if (currentExecutable.isFile() && currentExecutable.getName().equalsIgnoreCase("SchoolBell.exe")) {
                return currentExecutable;
            }
        }

        try {
            Path codeLocation = Path.of(UpdateService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path appDirectory = Files.isDirectory(codeLocation) ? codeLocation : codeLocation.getParent();
            if (appDirectory != null && "app".equalsIgnoreCase(String.valueOf(appDirectory.getFileName()))) {
                File main = appDirectory.getParent().resolve("SchoolBell.exe").toFile();
                if (main.isFile()) return main;
            }
        } catch (Exception ignored) {
            // Fall through to the working-directory lookup below.
        }

        File workingDirectoryMain = new File(System.getProperty("user.dir", "."), "SchoolBell.exe");
        return workingDirectoryMain.isFile() ? workingDirectoryMain : null;
    }
}
