package com.schoolbell.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

            JsonObject rootObj;
            try {
                JsonElement parsed = JsonParser.parseString(body);
                if (!parsed.isJsonObject()) {
                    handleFetchError("Помилка обробки даних (невірний формат JSON)");
                    return;
                }
                rootObj = parsed.getAsJsonObject();
            } catch (Exception e) {
                handleFetchError("Помилка обробки даних (невірний формат JSON)");
                return;
            }

            JsonObject dataObj = null;
            if (rootObj.has("raw") && rootObj.get("raw").isJsonObject()) {
                dataObj = rootObj.getAsJsonObject("raw");
            } else if (rootObj.has("states") && rootObj.get("states").isJsonObject()) {
                dataObj = rootObj.getAsJsonObject("states");
            }

            if (dataObj == null) {
                handleFetchError("API помилка: Відсутні дані у відповіді");
                return;
            }

            // Freshness Check
            if (rootObj.has("cachedat") && !rootObj.get("cachedat").isJsonNull()) {
                String cachedAtStr = rootObj.get("cachedat").getAsString();
                try {
                    LocalDateTime cachedAt = LocalDateTime.parse(cachedAtStr, DATE_TIME_FORMATTER);
                    if (cachedAt.isBefore(LocalDateTime.now().minusMinutes(5))) {
                        handleFetchError("Дані застаріли (останнє оновлення: " + cachedAtStr + ")");
                        return;
                    }
                } catch (Exception e) {
                    handleFetchError("Неможливо перевірити актуальність даних (помилка дати)");
                    return;
                }
            }

            String selectedRegion = configService.getSelectedRegionId();
            String selectedDistrict = configService.getSelectedDistrictId();

            if (selectedRegion == null || selectedRegion.isEmpty()) {
                logger.info("API Fetch successful, but no location selected.");
                handleSuccess();
                return;
            }

            JsonObject regionObj = findRegion(dataObj, selectedRegion);
            if (regionObj == null) {
                handleFetchError("Обраний регіон '" + selectedRegion + "' не знайдено в API");
                return;
            }

            boolean regionAlert = getBooleanField(regionObj, "alert", "alertnow", "enabled", "active");
            boolean currentAlert = checkDistrictAlert(regionObj, selectedDistrict, regionAlert);
            
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
            handleFetchError("Проблема з мережею: " + translateError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error in AirAlertService Live polling: ", e);
            handleFetchError("Внутрішня помилка сервісу: " + translateError(e.getMessage()));
        }
    }

    static JsonObject findRegion(JsonObject dataObj, String selectedRegion) {
        if (dataObj == null || selectedRegion == null || selectedRegion.isBlank()) return null;

        // 1. Direct key match
        if (dataObj.has(selectedRegion) && dataObj.get(selectedRegion).isJsonObject()) {
            return dataObj.getAsJsonObject(selectedRegion);
        }

        // 2. Iterate keys (both key-as-name and key-as-id with "name"/"title" inside)
        for (String key : dataObj.keySet()) {
            JsonElement elem = dataObj.get(key);
            if (!elem.isJsonObject()) continue;
            JsonObject obj = elem.getAsJsonObject();

            if (isNameMatch(key, selectedRegion)) {
                return obj;
            }

            for (String nameProp : new String[]{"name", "title", "region", "state"}) {
                if (obj.has(nameProp) && !obj.get(nameProp).isJsonNull()) {
                    String val = obj.get(nameProp).getAsString();
                    if (isNameMatch(val, selectedRegion)) {
                        return obj;
                    }
                }
            }
        }
        return null;
    }

    static boolean checkDistrictAlert(JsonObject regionObj, String selectedDistrict, boolean regionAlert) {
        if (regionObj == null || selectedDistrict == null || selectedDistrict.isBlank()) {
            return regionAlert;
        }

        for (String containerKey : new String[]{"districts", "community", "hromadas", "cities"}) {
            if (!regionObj.has(containerKey) || regionObj.get(containerKey).isJsonNull()) continue;
            JsonElement container = regionObj.get(containerKey);

            // Case A: JsonArray: [ {"name": "...", "alert": true}, ... ]
            if (container.isJsonArray()) {
                for (JsonElement item : container.getAsJsonArray()) {
                    if (!item.isJsonObject()) continue;
                    JsonObject dObj = item.getAsJsonObject();
                    for (String nameProp : new String[]{"name", "title", "district"}) {
                        if (dObj.has(nameProp) && !dObj.get(nameProp).isJsonNull()) {
                            if (isNameMatch(dObj.get(nameProp).getAsString(), selectedDistrict)) {
                                return getBooleanField(dObj, "alert", "alertnow", "enabled", "active");
                            }
                        }
                    }
                }
            }

            // Case B: JsonObject: { "Білоцерківський район": {"alert": true} } or { "1": {"name": "...", "alert": true} }
            if (container.isJsonObject()) {
                JsonObject dMap = container.getAsJsonObject();
                for (String dKey : dMap.keySet()) {
                    JsonElement dElem = dMap.get(dKey);
                    if (!dElem.isJsonObject()) continue;
                    JsonObject dObj = dElem.getAsJsonObject();

                    if (isNameMatch(dKey, selectedDistrict)) {
                        return getBooleanField(dObj, "alert", "alertnow", "enabled", "active");
                    }

                    for (String nameProp : new String[]{"name", "title", "district"}) {
                        if (dObj.has(nameProp) && !dObj.get(nameProp).isJsonNull()) {
                            if (isNameMatch(dObj.get(nameProp).getAsString(), selectedDistrict)) {
                                return getBooleanField(dObj, "alert", "alertnow", "enabled", "active");
                            }
                        }
                    }
                }
            }
        }

        return regionAlert;
    }

    static boolean isNameMatch(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        String norm1 = normalizeName(name1).toLowerCase(Locale.ROOT);
        String norm2 = normalizeName(name2).toLowerCase(Locale.ROOT);
        if (norm1.equals(norm2)) return true;

        String clean1 = norm1.replace("м.", "").replace("область", "").trim();
        String clean2 = norm2.replace("м.", "").replace("область", "").trim();
        if (!clean1.isEmpty() && clean1.equals(clean2)) return true;

        if (norm1.contains("крим") && norm2.contains("крим")) return true;
        if (norm1.contains("севастополь") && norm2.contains("севастополь")) return true;

        return false;
    }

    static boolean getBooleanField(JsonObject obj, String... fieldNames) {
        if (obj == null) return false;
        for (String field : fieldNames) {
            if (obj.has(field) && !obj.get(field).isJsonNull()) {
                try {
                    JsonElement elem = obj.get(field);
                    if (elem.isJsonPrimitive()) {
                        if (elem.getAsJsonPrimitive().isBoolean()) {
                            return elem.getAsBoolean();
                        }
                        if (elem.getAsJsonPrimitive().isNumber()) {
                            return elem.getAsInt() == 1;
                        }
                        String s = elem.getAsString().trim().toLowerCase(Locale.ROOT);
                        if (s.equals("true") || s.equals("1") || s.equals("active")) return true;
                        if (s.equals("false") || s.equals("0")) return false;
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    static String normalizeName(String name) {
        if (name == null) return "";
        return name.replace('’', '\'')
                   .replace('ʼ', '\'')
                   .replace('`', '\'')
                   .trim();
    }

    private String translateError(String msg) {
        if (msg == null) return "Невідома помилка";
        String lower = msg.toLowerCase();
        if (lower.contains("no route to host")) return "Відсутній маршрут до сервера (немає інтернету)";
        if (lower.contains("connection refused")) return "Сервер відхилив з'єднання";
        if (lower.contains("timed out")) return "Перевищено час очікування відповіді";
        if (lower.contains("unknown host")) return "Не вдалося знайти адресу сервера (проблема DNS)";
        if (lower.contains("network is unreachable")) return "Мережа недоступна";
        return msg;
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
}
