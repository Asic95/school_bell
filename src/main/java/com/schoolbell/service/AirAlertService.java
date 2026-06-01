package com.schoolbell.service;

import com.google.gson.Gson;
import com.schoolbell.MainApp;
import com.schoolbell.model.RegionDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AirAlertService {
    private static final Logger logger = LoggerFactory.getLogger(AirAlertService.class);
    private static final String API_URL = "https://ubilling.net.ua/aerialalerts/?source=default&raw";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MainApp mainApp;
    private final ConfigService configService;
    private final SignalService signalService;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTask;
    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    private boolean lastAlertState = false;
    private LocalDateTime lastErrorAnnouncement = LocalDateTime.MIN;
    private int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 3;
    private boolean isCurrentlyHealthy = true;

    public AirAlertService(MainApp mainApp, ConfigService configService, SignalService signalService, ScheduledExecutorService scheduler) {
        this.mainApp = mainApp;
        this.configService = configService;
        this.signalService = signalService;
        this.scheduler = scheduler;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void start() {
        if (!configService.isAirRaidAutomationEnabled()) {
            logger.info("Air raid automation disabled in config.");
            return;
        }
        if (pollingTask != null && !pollingTask.isCancelled()) return;
        
        pollingTask = scheduler.scheduleAtFixedRate(this::pollAndAct, 0, 6, TimeUnit.SECONDS);
        logger.info("AirAlertService started polling Live API (6s interval)");
        mainApp.addLog("Автоматизацію тривоги (Live API) активовано", "INFO");
    }

    public void stop() {
        if (pollingTask != null) {
            pollingTask.cancel(true);
            logger.info("AirAlertService stopped polling");
            mainApp.addLog("Автоматизацію тривоги зупинено", "INFO");
        }
    }

    private void pollAndAct() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                handleFetchError("API помилка: Код " + response.statusCode());
                return;
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                handleFetchError("API помилка: Отримано порожню відповідь");
                return;
            }

            AlertData data;
            try {
                data = gson.fromJson(body, AlertData.class);
            } catch (Exception e) {
                handleFetchError("Помилка обробки даних (невірний формат JSON)");
                return;
            }

            if (data == null || data.raw == null) {
                handleFetchError("API помилка: Відсутні дані у відповіді");
                return;
            }

            // Freshness Check
            try {
                LocalDateTime cachedAt = LocalDateTime.parse(data.cachedat, DATE_TIME_FORMATTER);
                if (cachedAt.isBefore(LocalDateTime.now().minusMinutes(5))) {
                    handleFetchError("Дані застаріли (останнє оновлення: " + data.cachedat + ")");
                    return;
                }
            } catch (Exception e) {
                handleFetchError("Неможливо перевірити актуальність даних (помилка дати)");
                return;
            }

            String selectedRegion = configService.getSelectedRegionId();
            String selectedDistrict = configService.getSelectedDistrictId();

            if (selectedRegion == null || selectedRegion.isEmpty()) {
                logger.info("API Fetch successful, but no location selected.");
                handleSuccess();
                return;
            }

            boolean currentAlert = false;
            RegionInfo regionInfo = data.raw.values().stream()
                    .filter(r -> r.name.equals(selectedRegion))
                    .findFirst().orElse(null);

            if (regionInfo != null) {
                if (selectedDistrict != null && !selectedDistrict.isEmpty()) {
                    currentAlert = regionInfo.districts.stream()
                            .anyMatch(d -> d.name.equals(selectedDistrict) && d.alert);
                } else {
                    currentAlert = regionInfo.alert;
                }
            } else {
                handleFetchError("Обраний регіон '" + selectedRegion + "' не знайдено в API");
                return;
            }
            
            handleSuccess();

            logger.info("API Check: [{} / {}] -> Status: {}", 
                    selectedRegion, 
                    (selectedDistrict != null && !selectedDistrict.isEmpty() ? selectedDistrict : "ALL"), 
                    (currentAlert ? "⚠️ ALERT ACTIVE" : "✅ CLEAR"));

            if (currentAlert && !lastAlertState) {
                String msg = "LIVE: ВИЯВЛЕНО ТРИВОГУ: " + selectedRegion + (selectedDistrict != null && !selectedDistrict.isEmpty() ? "/" + selectedDistrict : "");
                logger.warn(msg);
                mainApp.addLog(msg, "WARNING");
                signalService.runAirRaidSignal();
                lastAlertState = true;
            } else if (!currentAlert && lastAlertState) {
                String msg = "LIVE: ВІДБІЙ ТРИВОГИ: " + selectedRegion + (selectedDistrict != null && !selectedDistrict.isEmpty() ? "/" + selectedDistrict : "");
                logger.info(msg);
                mainApp.addLog(msg, "SUCCESS");
                signalService.runAirRaidClearSignal();
                lastAlertState = false;
            }
        } catch (IOException | InterruptedException e) {
            handleFetchError("Проблема з мережею: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in AirAlertService Live polling: ", e);
            handleFetchError("Внутрішня помилка сервісу: " + e.getMessage());
        }
    }

    private void handleFetchError(String error) {
        consecutiveFailures++;
        logger.error("AirAlertService error: {} (Consecutive failures: {})", error, consecutiveFailures);

        if (consecutiveFailures >= FAILURE_THRESHOLD) {
            if (isCurrentlyHealthy) {
                mainApp.addLog("УВАГА: " + error, "ERROR");
                isCurrentlyHealthy = false;
            }

            // Announce error via audio every 5 minutes if data is stale/missing
            if (LocalDateTime.now().isAfter(lastErrorAnnouncement.plusMinutes(5))) {
                signalService.playAutomationError();
                lastErrorAnnouncement = LocalDateTime.now();
            }
        }
    }

    private void handleSuccess() {
        if (!isCurrentlyHealthy) {
            mainApp.addLog("Зв'язок з API тривог відновлено", "SUCCESS");
            isCurrentlyHealthy = true;
        }
        consecutiveFailures = 0;
    }

    public List<String> getRegions() {
        List<String> special = List.of("м. Київ", "Крим", "Севастополь");
        List<String> regions = new ArrayList<>(RegionDirectory.UKRAINE_REGIONS.keySet());
        regions.removeAll(special);
        
        java.text.Collator collator = java.text.Collator.getInstance(Locale.of("uk", "UA"));
        regions.sort(collator);
        
        for (String s : special) {
            if (RegionDirectory.UKRAINE_REGIONS.containsKey(s)) {
                regions.add(s);
            }
        }
        return regions;
    }

    public List<String> getDistricts(String regionName) {
        return RegionDirectory.UKRAINE_REGIONS.getOrDefault(regionName, new ArrayList<>());
    }

    private static class AlertData { Map<String, RegionInfo> raw; String cachedat; }
    private static class RegionInfo { String name; boolean alert; List<DistrictInfo> districts; }
    private static class DistrictInfo { String name; boolean alert; }
}
