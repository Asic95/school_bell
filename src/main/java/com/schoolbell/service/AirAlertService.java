package com.schoolbell.service;

import com.google.gson.JsonArray;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AirAlertService {
    private static final Logger logger = LoggerFactory.getLogger(AirAlertService.class);
    private static final String API_URL = "https://schoolbell-alert.12asic12.workers.dev/";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MainApp mainApp;
    private final ConfigService configService;
    private final SignalService signalService;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTask;
    private final HttpClient httpClient;

    private boolean lastAlertState = false;
    private String lastAlertReason = "";
    private int clearConfirmationCount = 0;
    private static final int CLEAR_CONFIRMATION_THRESHOLD = 3; // 3 cycles x 6s = 18s debounce
    private LocalDateTime lastErrorAnnouncement = LocalDateTime.MIN;
    private int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 5;
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

    public static class AlertCheckResult {
        public final boolean isAlert;
        public final String reason;
        public final String level;

        public AlertCheckResult(boolean isAlert, String reason, String level) {
            this.isAlert = isAlert;
            this.reason = reason;
            this.level = level;
        }
    }

    private void pollAndAct() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "SchoolBell-App (Civil Protection Automation)")
                    .header("Accept", "application/json")
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

            JsonElement parsed;
            try {
                parsed = JsonParser.parseString(body);
            } catch (Exception e) {
                handleFetchError("Помилка обробки даних (невірний формат JSON)");
                return;
            }

            String selectedRegion = configService.getSelectedRegionId();
            String selectedDistrict = configService.getSelectedDistrictId();

            if (selectedRegion == null || selectedRegion.isEmpty()) {
                logger.info("API Fetch successful, but no location selected.");
                handleSuccess();
                return;
            }

            AlertCheckResult alertResult = checkAlerts(parsed, selectedRegion, selectedDistrict);
            boolean currentAlert = alertResult.isAlert;
            
            handleSuccess();

            String locationLabel = selectedRegion + (selectedDistrict != null && !selectedDistrict.isEmpty() ? "/" + selectedDistrict : "");
            logger.info("API Check: [{}] -> Status: {}{}", 
                    locationLabel, 
                    (currentAlert ? "⚠️ ALERT ACTIVE" : "✅ CLEAR"),
                    (alertResult.reason != null && !alertResult.reason.isBlank() ? " (" + alertResult.reason + ")" : ""));

            if (currentAlert) {
                // Reset clear confirmation counter immediately on active alert
                clearConfirmationCount = 0;

                if (!lastAlertState) {
                    lastAlertState = true;
                    lastAlertReason = alertResult.reason != null ? alertResult.reason : "";
                    
                    String reasonSuffix = !lastAlertReason.isBlank() ? " [" + lastAlertReason + "]" : "";
                    String msg = "LIVE: ВИЯВЛЕНО ТРИВОГУ: " + locationLabel + reasonSuffix;
                    logger.warn(msg);
                    mainApp.addLog(msg, "WARNING");
                    signalService.runAirRaidSignal();
                } else {
                    // Alert is already active - check for threat level change / escalation
                    String newReason = alertResult.reason != null ? alertResult.reason : "";
                    if (!newReason.isBlank() && !newReason.equalsIgnoreCase(lastAlertReason)) {
                        lastAlertReason = newReason;
                        String msg = "LIVE: ЗМІНА РІВНЯ ЗАГРОЗИ: " + locationLabel + " [" + newReason + "]";
                        logger.warn(msg);
                        mainApp.addLog(msg, "WARNING");
                        // Informational log only - do NOT re-trigger bell
                    }
                }
            } else {
                // currentAlert == false
                if (lastAlertState) {
                    clearConfirmationCount++;
                    logger.info("Alert clear check: confirmed {}/{} cycles", clearConfirmationCount, CLEAR_CONFIRMATION_THRESHOLD);
                    
                    if (clearConfirmationCount >= CLEAR_CONFIRMATION_THRESHOLD) {
                        lastAlertState = false;
                        lastAlertReason = "";
                        clearConfirmationCount = 0;

                        String msg = "LIVE: ВІДБІЙ ТРИВОГИ: " + locationLabel;
                        logger.info(msg);
                        mainApp.addLog(msg, "SUCCESS");
                        signalService.runAirRaidClearSignal();
                    }
                } else {
                    clearConfirmationCount = 0;
                }
            }
        } catch (IOException | InterruptedException e) {
            handleFetchError("Проблема з мережею: " + translateError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error in AirAlertService Live polling: ", e);
            handleFetchError("Внутрішня помилка сервісу: " + translateError(e.getMessage()));
        }
    }

    public static AlertCheckResult checkAlerts(JsonElement parsed, String selectedRegion, String selectedDistrict) {
        if (parsed == null || selectedRegion == null || selectedRegion.isBlank()) {
            return new AlertCheckResult(false, null, null);
        }

        // Format 1: UkraineAlarm / Cloudflare Worker (JsonArray of active locations)
        if (parsed.isJsonArray()) {
            return checkUkraineAlarmArray(parsed.getAsJsonArray(), selectedRegion, selectedDistrict);
        }

        // Format 2: Ubilling / Legacy (JsonObject with raw/states or direct regions)
        if (parsed.isJsonObject()) {
            JsonObject rootObj = parsed.getAsJsonObject();
            JsonObject dataObj = null;
            if (rootObj.has("raw") && rootObj.get("raw").isJsonObject()) {
                dataObj = rootObj.getAsJsonObject("raw");
            } else if (rootObj.has("states") && rootObj.get("states").isJsonObject()) {
                dataObj = rootObj.getAsJsonObject("states");
            } else {
                dataObj = rootObj;
            }

            JsonObject regionObj = findRegion(dataObj, selectedRegion);
            if (regionObj == null) {
                return new AlertCheckResult(false, null, null);
            }

            boolean regionAlert = isAlertActive(regionObj);
            boolean currentAlert = checkDistrictAlert(regionObj, selectedDistrict, regionAlert);
            String level = getAlertLevel(regionObj);
            return new AlertCheckResult(currentAlert, level != null ? "Рівень: " + level : null, level);
        }

        return new AlertCheckResult(false, null, null);
    }

    static AlertCheckResult checkUkraineAlarmArray(JsonArray array, String selectedRegion, String selectedDistrict) {
        boolean hasSpecificDistrict = selectedDistrict != null && !selectedDistrict.isBlank();
        List<String> regionDistricts = RegionDirectory.UKRAINE_REGIONS.getOrDefault(selectedRegion, Collections.emptyList());

        for (JsonElement elem : array) {
            if (!elem.isJsonObject()) continue;
            JsonObject item = elem.getAsJsonObject();

            String rName = item.has("regionName") && !item.get("regionName").isJsonNull() 
                    ? item.get("regionName").getAsString() : "";
            
            boolean hasActiveAlerts = false;
            String reason = null;
            String level = null;

            if (item.has("activeAlerts") && item.get("activeAlerts").isJsonArray()) {
                JsonArray alerts = item.getAsJsonArray("activeAlerts");
                if (!alerts.isEmpty()) {
                    hasActiveAlerts = true;
                    List<String> reasons = new ArrayList<>();
                    for (JsonElement aElem : alerts) {
                        if (!aElem.isJsonObject()) continue;
                        JsonObject aObj = aElem.getAsJsonObject();
                        if (aObj.has("activeAlertLevels") && aObj.get("activeAlertLevels").isJsonArray()) {
                            for (JsonElement lElem : aObj.getAsJsonArray("activeAlertLevels")) {
                                if (!lElem.isJsonObject()) continue;
                                JsonObject lObj = lElem.getAsJsonObject();
                                if (lObj.has("reason") && !lObj.get("reason").isJsonNull()) {
                                    String r = lObj.get("reason").getAsString().trim();
                                    if (!r.isEmpty() && !reasons.contains(r)) {
                                        reasons.add(r);
                                    }
                                }
                                if (level == null && lObj.has("alertLevel") && !lObj.get("alertLevel").isJsonNull()) {
                                    level = lObj.get("alertLevel").getAsString().trim();
                                }
                            }
                        }
                    }
                    if (!reasons.isEmpty()) {
                        Collections.sort(reasons);
                        reason = String.join(", ", reasons);
                    }
                }
            }

            if (!hasActiveAlerts) continue;

            if (hasSpecificDistrict) {
                // Match specific district OR whole region alert
                if (isNameMatch(rName, selectedDistrict) || isNameMatch(rName, selectedRegion)) {
                    return new AlertCheckResult(true, reason, level);
                }
            } else {
                // Match whole region OR any district in this region
                if (isNameMatch(rName, selectedRegion)) {
                    return new AlertCheckResult(true, reason, level);
                }
                for (String d : regionDistricts) {
                    if (isNameMatch(rName, d)) {
                        return new AlertCheckResult(true, reason, level);
                    }
                }
            }
        }

        return new AlertCheckResult(false, null, null);
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
        if (regionObj == null) return false;

        // Case A: Specific district is selected
        if (selectedDistrict != null && !selectedDistrict.isBlank()) {
            for (String containerKey : new String[]{"districts", "community", "hromadas", "cities"}) {
                if (!regionObj.has(containerKey) || regionObj.get(containerKey).isJsonNull()) continue;
                JsonElement container = regionObj.get(containerKey);

                if (container.isJsonArray()) {
                    for (JsonElement item : container.getAsJsonArray()) {
                        if (!item.isJsonObject()) continue;
                        JsonObject dObj = item.getAsJsonObject();
                        for (String nameProp : new String[]{"name", "title", "district"}) {
                            if (dObj.has(nameProp) && !dObj.get(nameProp).isJsonNull()) {
                                if (isNameMatch(dObj.get(nameProp).getAsString(), selectedDistrict)) {
                                    return isAlertActive(dObj);
                                }
                            }
                        }
                    }
                }

                if (container.isJsonObject()) {
                    JsonObject dMap = container.getAsJsonObject();
                    for (String dKey : dMap.keySet()) {
                        JsonElement dElem = dMap.get(dKey);
                        if (!dElem.isJsonObject()) continue;
                        JsonObject dObj = dElem.getAsJsonObject();

                        if (isNameMatch(dKey, selectedDistrict)) {
                            return isAlertActive(dObj);
                        }

                        for (String nameProp : new String[]{"name", "title", "district"}) {
                            if (dObj.has(nameProp) && !dObj.get(nameProp).isJsonNull()) {
                                if (isNameMatch(dObj.get(nameProp).getAsString(), selectedDistrict)) {
                                    return isAlertActive(dObj);
                                }
                            }
                        }
                    }
                }
            }
            return regionAlert;
        }

        // Case B: No specific district selected (whole oblast selected)
        if (regionAlert) return true;

        // If region-level enabled is false, check if any child district is in alarm
        for (String containerKey : new String[]{"districts", "community", "hromadas", "cities"}) {
            if (!regionObj.has(containerKey) || regionObj.get(containerKey).isJsonNull()) continue;
            JsonElement container = regionObj.get(containerKey);

            if (container.isJsonArray()) {
                for (JsonElement item : container.getAsJsonArray()) {
                    if (item.isJsonObject() && isAlertActive(item.getAsJsonObject())) {
                        return true;
                    }
                }
            }

            if (container.isJsonObject()) {
                JsonObject dMap = container.getAsJsonObject();
                for (String dKey : dMap.keySet()) {
                    JsonElement dElem = dMap.get(dKey);
                    if (dElem.isJsonObject() && isAlertActive(dElem.getAsJsonObject())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    static boolean isAlertActive(JsonObject obj) {
        if (obj == null) return false;
        if (getBooleanField(obj, "alert", "alertnow", "enabled", "active")) {
            return true;
        }
        if (obj.has("alert_level") && !obj.get("alert_level").isJsonNull()) {
            String level = obj.get("alert_level").getAsString().trim().toLowerCase(Locale.ROOT);
            if (level.equals("red") || level.equals("yellow") || level.equals("orange")) {
                return true;
            }
        }
        return false;
    }

    static String getAlertLevel(JsonObject obj) {
        if (obj == null) return null;
        if (obj.has("alert_level") && !obj.get("alert_level").isJsonNull()) {
            return obj.get("alert_level").getAsString().trim();
        }
        return null;
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
        clearConfirmationCount = 0; // Fail-Safe: Freeze alert state, do not allow clear counter to advance on error
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
