package com.schoolbell.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.schoolbell.model.RadioStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RadioStationService {
    private static final Logger logger = LoggerFactory.getLogger(RadioStationService.class);
    private static final String API_URL = "https://all.api.radio-browser.info/json/stations/bycountry/ukraine";
    private static final String CACHE_FILE = "radio_catalog_ua.json";
    
    private final Gson gson = new Gson();
    private List<RadioBrowserStation> catalogCache = new ArrayList<>();

    private static final List<String> PRIORITY_STATIONS = List.of(
            "Хіт FM", "Radio ROKS Ukrainian", "Kiss FM", "Lux FM", "Наше Радіо 107.9", 
            "Радіо Байрактар", "Армія FM 94.6", "Radio NV", "Радіо Промінь / Radio Promin", 
            "Перець FM", "Мелодія FM", "Radio Relax", "Lounge FM", "DJFM DANCE", 
            "NRJ Ukraine", "Радіо Максимум", "Avtoradio", "Радіо П'ятниця", 
            "Країна FM 100.0", "Lviv Wave Radio", "Радіо FM Галичина"
    );

    public RadioStationService() {
        loadCache();
    }

    /**
     * Station model from Radio Browser API
     */
    public record RadioBrowserStation(
        String name,
        String url_resolved,
        String codec,
        String favicon,
        int lastcheckok
    ) {}

    public void loadCache() {
        Path cachePath = Paths.get(PathService.getAppHomePath(), CACHE_FILE);
        if (Files.exists(cachePath)) {
            try (Reader reader = Files.newBufferedReader(cachePath)) {
                catalogCache = gson.fromJson(reader, new TypeToken<List<RadioBrowserStation>>(){}.getType());
                logger.info("Loaded {} stations from local catalog cache.", catalogCache.size());
            } catch (Exception e) {
                logger.error("Failed to load radio catalog cache", e);
            }
        }
    }

    public void refreshCatalog(Runnable onComplete) {
        new Thread(() -> {
            try {
                logger.info("Refreshing radio catalog from API...");
                URL url = new URL(API_URL);
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                
                try (InputStream is = conn.getInputStream();
                     Reader reader = new InputStreamReader(is)) {
                    
                    List<RadioBrowserStation> newCatalog = gson.fromJson(reader, new TypeToken<List<RadioBrowserStation>>(){}.getType());
                    
                    // Filter: Only MP3/AAC and currently working stations
                    catalogCache = newCatalog.stream()
                            .filter(s -> s.lastcheckok() == 1)
                            .filter(s -> s.codec() != null && (s.codec().equalsIgnoreCase("MP3") || s.codec().equalsIgnoreCase("AAC")))
                            .collect(Collectors.toList());

                    // Save to file
                    Path cachePath = Paths.get(PathService.getAppHomePath(), CACHE_FILE);
                    try (Writer writer = Files.newBufferedWriter(cachePath)) {
                        gson.toJson(catalogCache, writer);
                    }
                    
                    logger.info("Catalog refreshed. {} stations saved to cache.", catalogCache.size());
                    if (onComplete != null) {
                        javafx.application.Platform.runLater(onComplete);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to refresh radio catalog from API", e);
            }
        }, "RadioCatalog-Refresh-Thread").start();
    }

    public List<RadioBrowserStation> searchCatalog(String query) {
        if (query == null || query.isEmpty()) {
            // Priority first, then the rest
            List<RadioBrowserStation> result = new ArrayList<>();
            
            // 1. Add priority stations first (exact match)
            for (String priorityName : PRIORITY_STATIONS) {
                catalogCache.stream()
                        .filter(s -> s.name().equals(priorityName))
                        .findFirst()
                        .ifPresent(result::add);
            }
            
            // 2. Add remaining stations
            catalogCache.stream()
                    .filter(s -> !PRIORITY_STATIONS.contains(s.name()))
                    .forEach(result::add);
                    
            return result;
        }

        String q = query.toLowerCase();
        return catalogCache.stream()
                .filter(s -> s.name().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public int getTotalCatalogSize() {
        return catalogCache.size();
    }

    // --- FAVORITES (DB Wrapper) ---

    public List<RadioStation> getFavorites() {
        return DatabaseManager.getAllRadioStations();
    }

    public void addToFavorites(RadioBrowserStation browserStation) {
        RadioStation favorite = new RadioStation(null, browserStation.name(), browserStation.url_resolved(), browserStation.favicon());
        DatabaseManager.saveRadioStation(favorite);
    }

    public void removeFromFavorites(int id) {
        DatabaseManager.deleteRadioStation(id);
    }
}
