package com.schoolbell.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    // Durations
    private int regularBellDuration = 5;
    private int airRaidRingDuration = 3;
    private int airRaidPauseDuration = 1;
    private int emergencyDuration = 12;

    // Audio
    private String audioAirRaidPath = "";
    private boolean isAudioAirRaidEnabled = false;
    private String audioEmergencyPath = "";
    private boolean isAudioEmergencyEnabled = false;
    private String audioSilencePath = "";
    private boolean isAudioSilenceEnabled = false;
    private String selectedAudioDeviceName = "Системний за замовчуванням";
    private int systemVolume = 70;

    // Broadcast
    private boolean isBroadcastEnabled = false;
    private int broadcastPort = 8080;
    private String schoolName = "Ліцей №24";
    private String cityName = "Київ";
    private String announcementText = "Вітаємо у системі School Bell!";
    
    private String selectedScheduleName;
    private boolean isSimulationMode = false;

    public void loadConfig() {
        selectedScheduleName = DatabaseManager.getSetting("selectedSchedule", null);
        regularBellDuration = Integer.parseInt(DatabaseManager.getSetting("dur.regular", "5"));
        airRaidRingDuration = Integer.parseInt(DatabaseManager.getSetting("dur.arRing", "3"));
        airRaidPauseDuration = Integer.parseInt(DatabaseManager.getSetting("dur.arPause", "1"));
        emergencyDuration = Integer.parseInt(DatabaseManager.getSetting("dur.emergency", "12"));
        audioAirRaidPath = DatabaseManager.getSetting("audio.arPath", "");
        isAudioAirRaidEnabled = Boolean.parseBoolean(DatabaseManager.getSetting("audio.arEnabled", "false"));
        audioEmergencyPath = DatabaseManager.getSetting("audio.emPath", "");
        isAudioEmergencyEnabled = Boolean.parseBoolean(DatabaseManager.getSetting("audio.emEnabled", "false"));
        audioSilencePath = DatabaseManager.getSetting("audio.siPath", "");
        isAudioSilenceEnabled = Boolean.parseBoolean(DatabaseManager.getSetting("audio.siEnabled", "false"));
        selectedAudioDeviceName = DatabaseManager.getSetting("audio.device", "Системний за замовчуванням");
        systemVolume = Integer.parseInt(DatabaseManager.getSetting("audio.volume", "70"));
        isBroadcastEnabled = Boolean.parseBoolean(DatabaseManager.getSetting("broadcast.enabled", "false"));
        broadcastPort = Integer.parseInt(DatabaseManager.getSetting("broadcast.port", "8080"));
        schoolName = DatabaseManager.getSetting("school.name", "Ліцей №24");
        cityName = DatabaseManager.getSetting("school.city", "Київ");
        announcementText = DatabaseManager.getSetting("school.announcement", "Вітаємо у системі School Bell!");
        isSimulationMode = Boolean.parseBoolean(DatabaseManager.getSetting("system.simulation", "false"));
        logger.info("Configuration loaded from database.");
    }

    public void saveConfig() {
        DatabaseManager.saveSetting("selectedSchedule", selectedScheduleName != null ? selectedScheduleName : "");
        DatabaseManager.saveSetting("dur.regular", String.valueOf(regularBellDuration));
        DatabaseManager.saveSetting("dur.arRing", String.valueOf(airRaidRingDuration));
        DatabaseManager.saveSetting("dur.arPause", String.valueOf(airRaidPauseDuration));
        DatabaseManager.saveSetting("dur.emergency", String.valueOf(emergencyDuration));
        DatabaseManager.saveSetting("audio.arPath", audioAirRaidPath);
        DatabaseManager.saveSetting("audio.arEnabled", String.valueOf(isAudioAirRaidEnabled));
        DatabaseManager.saveSetting("audio.emPath", audioEmergencyPath);
        DatabaseManager.saveSetting("audio.emEnabled", String.valueOf(isAudioEmergencyEnabled));
        DatabaseManager.saveSetting("audio.siPath", audioSilencePath);
        DatabaseManager.saveSetting("audio.siEnabled", String.valueOf(isAudioSilenceEnabled));
        DatabaseManager.saveSetting("audio.device", selectedAudioDeviceName);
        DatabaseManager.saveSetting("audio.volume", String.valueOf(systemVolume));
        DatabaseManager.saveSetting("broadcast.enabled", String.valueOf(isBroadcastEnabled));
        DatabaseManager.saveSetting("broadcast.port", String.valueOf(broadcastPort));
        DatabaseManager.saveSetting("school.name", schoolName);
        DatabaseManager.saveSetting("school.city", cityName);
        DatabaseManager.saveSetting("school.announcement", announcementText);
        DatabaseManager.saveSetting("system.simulation", String.valueOf(isSimulationMode));
        logger.info("Configuration saved to database.");
    }

    // Getters and Setters
    public int getRegularBellDuration() { return regularBellDuration; }
    public void setRegularBellDuration(int regularBellDuration) { this.regularBellDuration = regularBellDuration; }
    public int getAirRaidRingDuration() { return airRaidRingDuration; }
    public void setAirRaidRingDuration(int airRaidRingDuration) { this.airRaidRingDuration = airRaidRingDuration; }
    public int getAirRaidPauseDuration() { return airRaidPauseDuration; }
    public void setAirRaidPauseDuration(int airRaidPauseDuration) { this.airRaidPauseDuration = airRaidPauseDuration; }
    public int getEmergencyDuration() { return emergencyDuration; }
    public void setEmergencyDuration(int emergencyDuration) { this.emergencyDuration = emergencyDuration; }
    public String getAudioAirRaidPath() { return audioAirRaidPath; }
    public void setAudioAirRaidPath(String audioAirRaidPath) { this.audioAirRaidPath = audioAirRaidPath; }
    public boolean isAudioAirRaidEnabled() { return isAudioAirRaidEnabled; }
    public void setAudioAirRaidEnabled(boolean audioAirRaidEnabled) { isAudioAirRaidEnabled = audioAirRaidEnabled; }
    public String getAudioEmergencyPath() { return audioEmergencyPath; }
    public void setAudioEmergencyPath(String audioEmergencyPath) { this.audioEmergencyPath = audioEmergencyPath; }
    public boolean isAudioEmergencyEnabled() { return isAudioEmergencyEnabled; }
    public void setAudioEmergencyEnabled(boolean audioEmergencyEnabled) { isAudioEmergencyEnabled = audioEmergencyEnabled; }
    public String getAudioSilencePath() { return audioSilencePath; }
    public void setAudioSilencePath(String audioSilencePath) { this.audioSilencePath = audioSilencePath; }
    public boolean isAudioSilenceEnabled() { return isAudioSilenceEnabled; }
    public void setAudioSilenceEnabled(boolean audioSilenceEnabled) { isAudioSilenceEnabled = audioSilenceEnabled; }
    public String getSelectedAudioDeviceName() { return selectedAudioDeviceName; }
    public void setSelectedAudioDeviceName(String selectedAudioDeviceName) { this.selectedAudioDeviceName = selectedAudioDeviceName; }
    public int getSystemVolume() { return systemVolume; }
    public void setSystemVolume(int systemVolume) { this.systemVolume = systemVolume; }
    public boolean isBroadcastEnabled() { return isBroadcastEnabled; }
    public void setBroadcastEnabled(boolean broadcastEnabled) { isBroadcastEnabled = broadcastEnabled; }
    public int getBroadcastPort() { return broadcastPort; }
    public void setBroadcastPort(int broadcastPort) { this.broadcastPort = broadcastPort; }
    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public String getAnnouncementText() { return announcementText; }
    public void setAnnouncementText(String announcementText) { this.announcementText = announcementText; }
    public String getSelectedScheduleName() { return selectedScheduleName; }
    public void setSelectedScheduleName(String selectedScheduleName) { this.selectedScheduleName = selectedScheduleName; }
    public boolean isSimulationMode() { return isSimulationMode; }
    public void setSimulationMode(boolean simulationMode) { isSimulationMode = simulationMode; }
}
