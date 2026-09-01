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

        JsonObject rawObj = root.getAsJsonObject("raw");
        assertTrue(rawObj.size() >= 20, "API must return regions (expected at least 20)");

        // Verify key regions exist and have valid structure
        List<String> sampleRegions = List.of("Київська область", "Львівська область", "Одеська область", "м. Київ");
        for (String region : sampleRegions) {
            assertTrue(rawObj.has(region), "API must include region: " + region);
            JsonObject regObj = rawObj.getAsJsonObject(region);
            assertTrue(regObj.has("enabled"), region + " must have 'enabled' boolean");
            assertDoesNotThrow(() -> regObj.get("enabled").getAsBoolean(), region + " 'enabled' must be boolean");

            // Verify districts if present
            if (regObj.has("districts")) {
                JsonElement distElem = regObj.get("districts");
                assertTrue(distElem.isJsonObject() || distElem.isJsonArray(), 
                        "districts must be either JsonObject or JsonArray for " + region);
            }
        }
    }
}
