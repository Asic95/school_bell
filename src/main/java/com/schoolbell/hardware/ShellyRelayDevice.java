package com.schoolbell.hardware;

import com.schoolbell.MainApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ShellyRelayDevice implements RelayDevice {
    private static final Logger logger = LoggerFactory.getLogger(ShellyRelayDevice.class);
    
    private final String ip;
    private final String name;
    private final MainApp mainApp;
    private final HttpClient httpClient;
    private Integer generation = null; // null = unknown, 1, 2, 3

    public ShellyRelayDevice(MainApp mainApp, String ip, String name) {
        this.mainApp = mainApp;
        this.ip = ip;
        this.name = name;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    private void detectGeneration() {
        if (generation != null) return;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + ip + "/shelly"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (response.body().contains("\"gen\":2")) generation = 2;
                else if (response.body().contains("\"gen\":3")) generation = 3;
                else generation = 1;
                logger.info("Shelly at {} detected as Gen {}", ip, generation);
            }
        } catch (Exception e) {
            logger.warn("Could not detect Shelly generation at {}: {}", ip, e.getMessage());
        }
    }

    @Override
    public void turnOn() {
        sendRequest(true);
    }

    @Override
    public void turnOff() {
        sendRequest(false);
    }

    private void sendRequest(boolean on) {
        if (mainApp != null && mainApp.getConfigService().isSimulationMode()) {
            logger.info("[SIMULATION] Shelly Relay ({}) command: {}", ip, on ? "ON" : "OFF");
            return;
        }

        detectGeneration();
        
        String url;
        if (generation != null && generation >= 2) {
            url = String.format("http://%s/rpc/Relay.Set?id=0&on=%b", ip, on);
        } else {
            url = String.format("http://%s/relay/0?turn=%s", ip, on ? "on" : "off");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(res -> {
                        if (res.statusCode() != 200) {
                            logger.error("Shelly ({}) returned status code: {}", ip, res.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        logger.error("Shelly ({}) request failed: {}", ip, ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Shelly ({}) unexpected error: {}", ip, e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        if (mainApp != null && mainApp.getConfigService().isSimulationMode()) return true;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + ip + "/shelly"))
                    .GET()
                    .timeout(Duration.ofMillis(800))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getDisplayName() {
        return String.format("WI-FI (SHELLY): %s [%s]", name, ip);
    }

    @Override
    public void close() {
        // No persistent connection to close
    }

    public String getIp() { return ip; }
}
