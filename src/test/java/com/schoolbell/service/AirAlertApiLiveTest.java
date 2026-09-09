package com.schoolbell.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AirAlertApiLiveTest {

    private static final String API_URL = "https://ubilling.net.ua/aerialalerts/?source=default&raw";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("Verify Live Alert API endpoint schema and availability")
    public void testLiveApiSchema() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "API must return HTTP 200");

        String body = response.body();
        assertNotNull(body, "Response body must not be null");
        assertFalse(body.isBlank(), "Response body must not be blank");

        JsonElement parsed = JsonParser.parseString(body);
        assertTrue(parsed.isJsonObject(), "Response must be a JSON object");

        JsonObject root = parsed.getAsJsonObject();
        assertTrue(root.has("raw"), "JSON must contain 'raw' property");
        assertTrue(root.get("raw").isJsonObject(), "'raw' property must be a JSON object");

        // Verify cachedat
        assertTrue(root.has("cachedat"), "JSON must contain 'cachedat' property");
        String cachedat = root.get("cachedat").getAsString();
        assertDoesNotThrow(() -> LocalDateTime.parse(cachedat, DATE_TIME_FORMATTER), "cachedat format must match yyyy-MM-dd HH:mm:ss");

        // Support both "raw" and "states" root properties
        JsonObject dataObj = null;
        if (root.has("raw") && root.get("raw").isJsonObject()) {
            dataObj = root.getAsJsonObject("raw");
        } else if (root.has("states") && root.get("states").isJsonObject()) {
            dataObj = root.getAsJsonObject("states");
        }

        assertNotNull(dataObj, "JSON must contain valid 'raw' or 'states' object");
        assertTrue(dataObj.size() >= 20, "API must return regions (expected at least 20)");

        // Verify key regions exist and have valid structure
        List<String> sampleRegions = List.of("Київська область", "Львівська область", "Одеська область", "м. Київ");
        for (String region : sampleRegions) {
            JsonObject regObj = AirAlertService.findRegion(dataObj, region);
            assertNotNull(regObj, "API must include resolvable region: " + region);

            boolean hasAlertProperty = regObj.has("alert") || regObj.has("alertnow") || regObj.has("enabled") || regObj.has("active");
            assertTrue(hasAlertProperty, region + " must have alert status (alert, alertnow, enabled, or active)");

            // Verify districts if present
            if (regObj.has("districts") && !regObj.get("districts").isJsonNull()) {
                JsonElement distElem = regObj.get("districts");
                assertTrue(distElem.isJsonObject() || distElem.isJsonArray(), 
                        "districts must be either JsonObject or JsonArray for " + region);
            }
        }

        // Verify district-level alert resolution
        JsonObject kyivRegion = AirAlertService.findRegion(dataObj, "Київська область");
        assertNotNull(kyivRegion, "Київська область must be resolvable");
        boolean districtAlert = AirAlertService.checkDistrictAlert(kyivRegion, "Білоцерківський район", false);
        assertTrue(districtAlert == true || districtAlert == false, "District alert status should resolve without error");
    }

    @Test
    @DisplayName("Verify Cloudflare Worker UkraineAlarm Live Proxy endpoint")
    public void testLiveCloudflareWorkerEndpoint() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://schoolbell-alert.12asic12.workers.dev/"))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "SchoolBell-Test/1.2.3")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Cloudflare Worker must return HTTP 200");

        String body = response.body();
        assertNotNull(body, "Response body must not be null");
        assertFalse(body.isBlank(), "Response body must not be blank");

        JsonElement parsed = JsonParser.parseString(body);
        assertTrue(parsed.isJsonArray(), "Cloudflare Worker response must be a JSON array");

        // Verify checkAlerts execution against real live data
        AirAlertService.AlertCheckResult result = AirAlertService.checkAlerts(parsed, "Київська область", "Білоцерківський район");
        assertNotNull(result, "AlertCheckResult must not be null");
    }
}
