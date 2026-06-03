package com.schoolbell.service;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.schoolbell.MainApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UpdateService {
    private static final Logger logger = LoggerFactory.getLogger(UpdateService.class);
    private static final String MANIFEST_URL = "https://raw.githubusercontent.com/Asic95/school_bell/master/updates.json";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Gson gson = new Gson();
    private Consumer<String> journalConsumer;

    public void setJournalConsumer(Consumer<String> consumer) {
        this.journalConsumer = consumer;
    }

    private void logToJournal(String message) {
        logger.info(message);
        if (journalConsumer != null) {
            journalConsumer.accept(message);
        }
    }

    public record UpdateManifest(
            @SerializedName("latest_version") String latestVersion,
            @SerializedName("release_date") String releaseDate,
            boolean critical,
            List<String> changelog,
            @SerializedName("download_url") String downloadUrl,
            String checksum
    ) {}

    public CompletableFuture<UpdateManifest> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logToJournal("Перевірка оновлень за адресою: " + MANIFEST_URL);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(MANIFEST_URL))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    UpdateManifest manifest = gson.fromJson(response.body(), UpdateManifest.class);
                    String currentVersion = MainApp.VERSION;
                    String latestVersion = manifest.latestVersion();
                    
                    logToJournal("Поточна версія: " + currentVersion + ", Доступна: " + latestVersion);
                    
                    if (isNewerVersion(latestVersion)) {
                        logToJournal("Виявлено нову версію! Потрібне оновлення.");
                        return manifest;
                    } else {
                        logToJournal("У вас встановлена остання версія системи.");
                    }
                } else {
                    logToJournal("Помилка отримання маніфесту. Код сервера: " + response.statusCode());
                }
            } catch (Exception e) {
                logToJournal("Помилка під час перевірки оновлень: " + e.getMessage());
            }
            return null;
        });
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

    public CompletableFuture<File> downloadUpdate(UpdateManifest manifest, Consumer<Double> progressConsumer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(manifest.downloadUrl()))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new IOException("Server returned status code " + response.statusCode());
                }

                long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                Path tempFile = Files.createTempFile("schoolbell_update_", ".exe");
                
                try (InputStream is = response.body();
                     OutputStream os = new FileOutputStream(tempFile.toFile())) {
                    
                    byte[] buffer = new byte[8192];
                    long downloadedBytes = 0;
                    int bytesRead;
                    
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;
                        if (totalBytes > 0) {
                            progressConsumer.accept((double) downloadedBytes / totalBytes);
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

                return tempFile.toFile();
            } catch (Exception e) {
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

    public void installUpdate(File installerFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    installerFile.getAbsolutePath(),
                    "/VERYSILENT",
                    "/CLOSEAPPLICATIONS",
                    "/RESTARTAPPLICATIONS"
            );
            pb.start();
            System.exit(0);
        } catch (IOException e) {
            logger.error("Failed to launch installer: {}", e.getMessage());
        }
    }
}
