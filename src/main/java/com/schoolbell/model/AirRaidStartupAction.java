package com.schoolbell.model;

/**
 * Modes of action when an air raid alert is detected at application startup.
 */
public enum AirRaidStartupAction {
    BELL_AND_AUDIO("Дзвінок і аудіосистема", "3 цикли шкільного дзвінка та звукова сирена", true, true),
    BELL_ONLY("Тільки шкільний дзвінок", "3 цикли шкільного дзвінка (аудіосистема мовчить)", true, false),
    AUDIO_ONLY("Тільки аудіосистема", "Звукове сповіщення через аудіосистему без дзвінка", false, true),
    SILENT("Тихий режим", "Фіксація тривоги без звукових сигналів", false, false);

    private final String title;
    private final String subtitle;
    private final boolean includesBell;
    private final boolean includesAudio;

    AirRaidStartupAction(String title, String subtitle, boolean includesBell, boolean includesAudio) {
        this.title = title;
        this.subtitle = subtitle;
        this.includesBell = includesBell;
        this.includesAudio = includesAudio;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public boolean includesBell() {
        return includesBell;
    }

    public boolean includesAudio() {
        return includesAudio;
    }

    @Override
    public String toString() {
        return title;
    }

    public static AirRaidStartupAction fromString(String val) {
        if (val == null || val.isBlank()) {
            return BELL_AND_AUDIO;
        }
        for (AirRaidStartupAction action : values()) {
            if (action.name().equalsIgnoreCase(val.trim())) {
                return action;
            }
        }
        return BELL_AND_AUDIO;
    }
}
