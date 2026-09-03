package com.schoolbell.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AirAlertServiceTest {

    @Test
    @DisplayName("Should parse Numeric IDs Schema with array districts (UBilling current raw format)")
    public void testNumericIdsSchema() {
        String json = """
        {
          "raw": {
            "1": {
              "name": "Вінницька область",
              "alert": false,
              "districts": [
                { "name": "Вінницький район", "alert": false }
              ]
            },
            "10": {
              "name": "Київська область",
              "alert": true,
              "districts": [
                { "name": "Білоцерківський район", "alert": true },
                { "name": "Бориспільський район", "alert": false }
              ]
            },
            "25": {
              "name": "м. Київ",
              "alert": false,
              "districts": []
            }
          }
        }
        """;

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject dataObj = root.getAsJsonObject("raw");

        JsonObject kyivRegion = AirAlertService.findRegion(dataObj, "Київська область");
        assertNotNull(kyivRegion);
        assertTrue(AirAlertService.getBooleanField(kyivRegion, "alert", "alertnow", "enabled", "active"));

        // District with alert true
        boolean btsAlert = AirAlertService.checkDistrictAlert(kyivRegion, "Білоцерківський район", false);
        assertTrue(btsAlert);

        // District with alert false
        boolean boryspilAlert = AirAlertService.checkDistrictAlert(kyivRegion, "Бориспільський район", true);
        assertFalse(boryspilAlert);

        // Region without alert
        JsonObject vinnytsia = AirAlertService.findRegion(dataObj, "Вінницька область");
        assertNotNull(vinnytsia);
        assertFalse(AirAlertService.getBooleanField(vinnytsia, "alert", "alertnow", "enabled", "active"));

        // City Kyiv
        JsonObject kyivCity = AirAlertService.findRegion(dataObj, "м. Київ");
        assertNotNull(kyivCity);
        assertFalse(AirAlertService.getBooleanField(kyivCity, "alert", "alertnow", "enabled", "active"));
    }

    @Test
    @DisplayName("Should parse Direct Region Names Schema with map districts (UBilling previous raw format)")
    public void testDirectRegionNamesSchema() {
        String json = """
        {
          "raw": {
            "Київська область": {
              "enabled": true,
              "districts": {
                "Білоцерківський район": { "enabled": true },
                "Бориспільський район": { "enabled": false }
              }
            },
            "м. Київ": {
              "enabled": false
            }
          }
        }
        """;

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject dataObj = root.getAsJsonObject("raw");

        JsonObject kyivRegion = AirAlertService.findRegion(dataObj, "Київська область");
        assertNotNull(kyivRegion);
        assertTrue(AirAlertService.getBooleanField(kyivRegion, "alert", "alertnow", "enabled", "active"));

        boolean btsAlert = AirAlertService.checkDistrictAlert(kyivRegion, "Білоцерківський район", false);
        assertTrue(btsAlert);

        boolean boryspilAlert = AirAlertService.checkDistrictAlert(kyivRegion, "Бориспільський район", true);
        assertFalse(boryspilAlert);
    }

    @Test
    @DisplayName("Should parse UBilling states format (Standard default format without &raw)")
    public void testStatesSchema() {
        String json = """
        {
          "source": "Mørk Skogen API (default)",
          "cachedat": "2026-09-03 08:32:39",
          "states": {
            "Київська область": { "alertnow": true, "changed": "2026-09-03 04:21:44" },
            "Львівська область": { "alertnow": false, "changed": "1970-01-01 03:00:00" },
            "м. Київ": { "alertnow": true, "changed": "2026-09-03 08:12:08" }
          }
        }
        """;

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject dataObj = root.getAsJsonObject("states");

        JsonObject kyivRegion = AirAlertService.findRegion(dataObj, "Київська область");
        assertNotNull(kyivRegion);
        assertTrue(AirAlertService.getBooleanField(kyivRegion, "alert", "alertnow", "enabled", "active"));

        JsonObject lvivRegion = AirAlertService.findRegion(dataObj, "Львівська область");
        assertNotNull(lvivRegion);
        assertFalse(AirAlertService.getBooleanField(lvivRegion, "alert", "alertnow", "enabled", "active"));
    }
}
