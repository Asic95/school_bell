package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.service.AcademicService;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.SignalService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.CacheHint;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.schoolbell.ui.CardFactory.createSmallInfoCard;
import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class DashboardView {
    private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private static final String MODERN_PROGRESS_STYLE = 
        ".progress-bar { -fx-background-color: transparent; -fx-padding: 0; -fx-background-insets: 0; } " +
        ".progress-bar > .track { -fx-background-color: " + COLOR_SURFACE_SUBTLE + "; -fx-background-radius: 10; -fx-background-insets: 0; } " +
        ".progress-bar > .bar { -fx-background-color: linear-gradient(to right, " + COLOR_PRIMARY + ", " + COLOR_PURPLE + "); -fx-background-radius: 10; -fx-background-insets: 0; }";

    private final MainApp mainApp;
    private final ConfigService config;
    private final SignalService signalService;
    private final AcademicService academicService;
    private final DashboardDataModel dataModel;

    private Label currentTimeLabel;
    private Label relayStatusLabel;
    private Label relaySubtext;
    private Circle relayIndicator;
    private Label countdownLabel;
    private Label nextBellTypeLabel;
    
    private Button airRaidBtn;
    private Button emergencyBtn;
    
    private Label curLessonNumLabel;
    private Label curLessonStatusBadge;
    private Label curLessonTimeLabel;
    private Label curLessonSubjectLabel;
    private ProgressBar curLessonProgress;
    private Label curLessonProgressText;
    
    private Label nextLessonNumLabel;
    private Label nextLessonStatusBadge;
    private Label nextLessonTimeLabel;
    private Label nextLessonSubjectLabel;

    private Label mediaValue;
    private Button stopMediaBtn;
    private Label activeScheduleValue;
    private Label topActiveScheduleLabel;

    private int currentVolumeValue;
    private Label volStatusLabel;
    private HBox volumePresetBox;
    
    private int lastLessonIdx = -2;
    private boolean lastIsBreak = false;
    private String lastScheduleName = "__INITIAL__";

    public DashboardView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.signalService = mainApp.getSignalService();
        this.academicService = mainApp.getAcademicService();
        this.dataModel = new DashboardDataModel(mainApp);
    }

    public Node build() {
        lastLessonIdx = -2;
        lastIsBreak = false;
        lastScheduleName = "__INITIAL__";

        countdownLabel = new Label("00:00:00");
        nextBellTypeLabel = new Label();

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(25));
        grid.setStyle(DEPTH_1);
        
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(55);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(45);
        grid.getColumnConstraints().addAll(col1, col2);

        // --- TOP ROW ---
        currentTimeLabel = new Label("00:00:00");
        currentTimeLabel.setStyle("-fx-font-size: 52px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        Label timeSubtext = new Label("NTP СИНХРОНІЗОВАНО");
        timeSubtext.setStyle("-fx-font-size: 10px; -fx-text-fill: " + COLOR_SUCCESS + "; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        Circle ntpDot = new Circle(4, Color.web(COLOR_SUCCESS));
        VBox timeCard = new VBox(5, new Label("ПОТОЧНИЙ ЧАС"), currentTimeLabel, new HBox(8, ntpDot, timeSubtext));
        ((Label)timeCard.getChildren().get(0)).setStyle(HEADER_STYLE);
        timeCard.setPadding(new Insets(25));
        timeCard.setStyle(SOFT_CARD);
        // Caching disabled for high-frequency updates (clock)
        grid.add(timeCard, 0, 0);

        relayIndicator = new Circle(8, Color.web(COLOR_DANGER));
        relayStatusLabel = new Label("НЕМАЄ ЗВ'ЯЗКУ");
        relayStatusLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_DANGER + ";");
        relaySubtext = new Label("ПЕРЕВІРТЕ ПІДКЛЮЧЕННЯ");
        relaySubtext.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");
        
        HBox statusRow = new HBox(12, relayIndicator, relayStatusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox relayContent = new VBox(4, statusRow, relaySubtext);
        relayContent.setAlignment(Pos.CENTER_LEFT);
        
        Label relayHeader = new Label("СТАТУС РЕЛЕ");
        relayHeader.setStyle(HEADER_STYLE);
        
        Region rSpacer = new Region();
        VBox.setVgrow(rSpacer, Priority.ALWAYS);
        
        VBox relayCard = new VBox(5, relayHeader, rSpacer, relayContent);
        relayCard.setPadding(new Insets(25));
        relayCard.setStyle(SOFT_CARD);
        // Caching disabled for relay status updates
        grid.add(relayCard, 1, 0);

        // --- MIDDLE ROW ---
        VBox heroCard = new VBox(25);
        heroCard.setPadding(new Insets(30));
        heroCard.setStyle(SOFT_CARD);
        // Caching disabled for progress bar and countdown
        
        HBox heroHeader = new HBox(15);
        heroHeader.setAlignment(Pos.CENTER_LEFT);
        Label flowHeader = new Label("ПОТОЧНИЙ СТАН ТА РОЗКЛАД");
        flowHeader.setStyle(HEADER_STYLE);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        topActiveScheduleLabel = new Label(config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        topActiveScheduleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-background-color: " + COLOR_BLUE_LIGHT + "; -fx-padding: 6 14; -fx-background-radius: 20;");
        heroHeader.getChildren().addAll(flowHeader, headerSpacer, topActiveScheduleLabel);

        HBox currentSessionInfo = new HBox(30);
        currentSessionInfo.setAlignment(Pos.CENTER_LEFT);
        
        VBox sessionIcon = new VBox(createSVGIcon(ICON_BOOK, Color.web(COLOR_PRIMARY), 48));
        sessionIcon.setAlignment(Pos.CENTER);
        sessionIcon.setPrefSize(84, 84);
        sessionIcon.setStyle(ICON_BADGE_STYLE);

        VBox sessionText = new VBox(6);
        curLessonNumLabel = new Label("--");
        curLessonNumLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        curLessonStatusBadge = new Label("ОЧІКУВАННЯ");
        curLessonStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_NEUTRAL + "; -fx-padding: 4 12; -fx-background-radius: 12;");
        HBox titleRow = new HBox(15, curLessonNumLabel, curLessonStatusBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        curLessonSubjectLabel = new Label("Завантаження даних...");
        curLessonSubjectLabel.setWrapText(true);
        curLessonSubjectLabel.setMaxWidth(400);
        curLessonSubjectLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_SLATE + ";");
        sessionText.getChildren().addAll(titleRow, curLessonSubjectLabel);
        
        Region sessionSpacer = new Region();
        HBox.setHgrow(sessionSpacer, Priority.ALWAYS);
        
        VBox countdownBox = new VBox(2);
        countdownBox.setAlignment(Pos.CENTER_RIGHT);
        Label countdownTitle = new Label("ДО НАСТУПНОГО ДЗВІНКА");
        countdownTitle.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        countdownLabel = new Label("00:00:00");
        countdownLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + ";");
        nextBellTypeLabel = new Label("—");
        nextBellTypeLabel.setWrapText(true);
        nextBellTypeLabel.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
        nextBellTypeLabel.setMaxWidth(220);
        nextBellTypeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_SLATE_LIGHT + ";");
        countdownBox.getChildren().addAll(countdownTitle, countdownLabel, nextBellTypeLabel);
        
        currentSessionInfo.getChildren().addAll(sessionIcon, sessionText, sessionSpacer, countdownBox);

        VBox progressArea = new VBox(12);
        curLessonProgress = new ProgressBar(0);
        curLessonProgress.setMaxWidth(Double.MAX_VALUE);
        curLessonProgress.setPrefHeight(20); 
        curLessonProgress.getStylesheets().add("data:text/css," + MODERN_PROGRESS_STYLE.replace(" ", "%20"));
        
        HBox progressLabels = new HBox();
        curLessonTimeLabel = new Label("--:-- – --:--");
        curLessonTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_SLATE + ";");
        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        curLessonProgressText = new Label("0%");
        curLessonProgressText.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + ";");
        progressLabels.getChildren().addAll(curLessonTimeLabel, pSpacer, curLessonProgressText);
        progressArea.getChildren().addAll(progressLabels, curLessonProgress);

        heroCard.getChildren().addAll(heroHeader, currentSessionInfo, progressArea);
        grid.add(heroCard, 0, 1);

        VBox nextEventCard = new VBox(25);
        nextEventCard.setPadding(new Insets(30));
        nextEventCard.setStyle(SOFT_CARD);
        // Caching disabled for next lesson countdown
        
        Label nextHeader = new Label("НАСТУПНА ПОДІЯ");
        nextHeader.setStyle(HEADER_STYLE);
        
        VBox nextContent = new VBox(15);
        nextContent.setAlignment(Pos.CENTER);
        
        VBox nextIconCircle = new VBox(createSVGIcon(ICON_CLOCK, Color.web(COLOR_SUCCESS), 40));
        nextIconCircle.setAlignment(Pos.CENTER);
        nextIconCircle.setPrefSize(74, 74);
        nextIconCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, " + COLOR_SURFACE_GLASS_START + ", " + COLOR_SURFACE_GLASS_END + "); -fx-background-radius: 20;");
        
        nextLessonNumLabel = new Label("--");
        nextLessonNumLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        
        VBox nextInfoText = new VBox(4);
        nextInfoText.setAlignment(Pos.CENTER);
        nextLessonSubjectLabel = new Label("Очікування...");
        nextLessonSubjectLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        nextLessonTimeLabel = new Label("--:-- – --:--");
        nextLessonTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_SLATE + ";");
        nextInfoText.getChildren().addAll(nextLessonSubjectLabel, nextLessonTimeLabel);
        
        nextLessonStatusBadge = new Label("ПЕРЕРВА");
        nextLessonStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 5 15; -fx-background-radius: 20;");
        
        nextContent.getChildren().addAll(nextIconCircle, nextLessonNumLabel, nextInfoText, nextLessonStatusBadge);
        
        nextEventCard.getChildren().addAll(nextHeader, nextContent);
        grid.add(nextEventCard, 1, 1);

        HBox actionsBox = new HBox(20);
        airRaidBtn = createActionButton("ПОВІТРЯНА ТРИВОГА", "Активація режиму загальної небезпеки", ICON_AIR_RAID, GRADIENT_WARNING);
        airRaidBtn.setOnAction(e -> {
            airRaidBtn.setDisable(true);
            if ("AIR_RAID".equals(signalService.getCurrentAlertType())) {
                signalService.runAirRaidClearSignal();
            } else {
                signalService.runAirRaidSignal();
            }
        });
        HBox.setHgrow(airRaidBtn, Priority.ALWAYS);
        
        emergencyBtn = createActionButton("ЕКСТРЕНА СИТУАЦІЯ", "Сигнал термінової евакуації закладу", ICON_LIFEBUOY, GRADIENT_DANGER);
        emergencyBtn.setOnAction(e -> {
            emergencyBtn.setDisable(true);
            if ("EMERGENCY".equals(signalService.getCurrentAlertType())) {
                signalService.runEmergencyClearSignal();
            } else {
                signalService.runEmergencySignal();
            }
        });
        HBox.setHgrow(emergencyBtn, Priority.ALWAYS);
        
        actionsBox.getChildren().addAll(airRaidBtn, emergencyBtn);
        
        VBox quickActionsCard = new VBox(20, new Label("ШВИДКІ ДІЇ"), actionsBox);
        ((Label)quickActionsCard.getChildren().get(0)).setStyle(HEADER_STYLE);
        quickActionsCard.setPadding(new Insets(25));
        quickActionsCard.setStyle(SOFT_CARD);
        quickActionsCard.setCache(true);
        quickActionsCard.setCacheHint(CacheHint.SPEED);
        grid.add(quickActionsCard, 0, 2, 2, 1);

        HBox infoRow = new HBox(20);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        
        activeScheduleValue = new Label(config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        applyInfoValueStyle(activeScheduleValue, COLOR_NAVY);
        VBox schCard = createSmallInfoCard("АКТИВНИЙ РОЗКЛАД", activeScheduleValue, "Змінити", () -> new ScheduleQuickSelectorDialog(mainApp).display(), ICON_CALENDAR, COLOR_BLUE_LIGHT, COLOR_PRIMARY, true, 0, null);
        
        currentVolumeValue = normalizeVolume(config.getSystemVolume());
        volStatusLabel = new Label(currentVolumeValue + "%");
        applyInfoValueStyle(volStatusLabel, COLOR_NAVY);
        
        volumePresetBox = new HBox(0); // Seamless segmented look
        volumePresetBox.setAlignment(Pos.CENTER_LEFT);
        volumePresetBox.setMaxWidth(Region.USE_PREF_SIZE); // Prevent stretching
        volumePresetBox.setStyle(PREMIUM_TOGGLE_CONTAINER + "-fx-padding: 2;");
        
        int[] presets = {0, 25, 50, 75, 100};
        for (int p : presets) {
            Button pb = new Button(p + "");
            pb.setPrefHeight(28);
            pb.setMinWidth(38);
            pb.setUserData(p);
            pb.setOnAction(e -> {
                currentVolumeValue = p;
                safeSetText(volStatusLabel, p + "%");
                updateVolumeStyle();
                config.setSystemVolume(p);
                mainApp.getSystemService().setWindowsSystemVolume(p);
                mainApp.saveConfig();
            });
            volumePresetBox.getChildren().add(pb);
        }
        updateVolumeStyle();

        VBox volContent = new VBox(5, volStatusLabel, volumePresetBox);
        VBox volCard = createSmallInfoCard("СИСТЕМНА ГУЧНІСТЬ", volContent, null, null, ICON_VOLUME, COLOR_GREEN_LIGHT, COLOR_SUCCESS, true, 0, null);
        
        Label brStatusLabel = new Label(config.isBroadcastEnabled() ? "Увімкнено" : "Вимкнено");
        applyInfoValueStyle(brStatusLabel, COLOR_NAVY);
        VBox brCard = createSmallInfoCard("ТРАНСЛЯЦІЯ ДАШБОРДУ", brStatusLabel, "Відкрити в браузері", () -> mainApp.getHostServices().showDocument("http://localhost:" + (config.getBroadcastPort())), ICON_MONITOR, COLOR_PURPLE_LIGHT, COLOR_INDIGO_SOFT, true, 0, null);
        
        mediaValue = new Label("Очікування...");
        applyInfoValueStyle(mediaValue, COLOR_NAVY);
        
        VBox mediaCard = createSmallInfoCard("МЕДІА-ЕФІР", mediaValue, "Управління", () -> new MediaQuickControlDialog(mainApp).display(), ICON_AIRPLAY, COLOR_TANGERINE_LIGHT, COLOR_TANGERINE, true, 0, null);
        
        infoRow.getChildren().addAll(schCard, volCard, brCard, mediaCard);
        for (Node n : infoRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        grid.add(infoRow, 0, 3, 2, 1);

        ScrollPane mainScroll = new ScrollPane(grid);
        mainScroll.setFitToWidth(true); 
        mainScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        
        update(LocalTime.now());
        return mainScroll;
    }

    private void applyInfoValueStyle(Label label, String color) {
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + color + ";");
        label.setWrapText(true);
        label.setMaxWidth(200);
    }

    private void safeSetText(Label label, String text) {
        if (label == null || text == null) return;
        if (!text.equals(label.getText())) {
            label.setText(text);
        }
    }

    private void safeSetProgress(ProgressBar bar, double progress) {
        if (bar == null) return;
        if (Math.abs(bar.getProgress() - progress) > 0.001) {
            bar.setProgress(progress);
        }
    }

    private int normalizeVolume(int value) {
        if (value <= 12) return 0;
        if (value <= 37) return 25;
        if (value <= 62) return 50;
        if (value <= 87) return 75;
        return 100;
    }

    private void updateVolumeStyle() {
        if (volumePresetBox == null) return;
        for (Node n : volumePresetBox.getChildren()) {
            if (n instanceof Button b) {
                int val = (int) b.getUserData();
                if (val == currentVolumeValue) {
                    b.setStyle(PREMIUM_TOGGLE_ACTIVE);
                    b.setOnMouseEntered(null);
                    b.setOnMouseExited(null);
                } else {
                    b.setStyle(PREMIUM_TOGGLE_INACTIVE);
                    b.setOnMouseEntered(e -> b.setStyle(PREMIUM_TOGGLE_INACTIVE + "-fx-background-color: " + TR_WHITE_08 + ";"));
                    b.setOnMouseExited(e -> b.setStyle(PREMIUM_TOGGLE_INACTIVE));
                }
            }
        }
    }

    public void update(LocalTime now) {
        safeSetText(currentTimeLabel, now.format(HH_MM_SS));
        
        if (config.isSimulationMode()) {
            safeSetText(relayStatusLabel, "РЕЖИМ СИМУЛЯЦІЇ");
            relayStatusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_INDIGO + ";");
            relayIndicator.setFill(Color.web(COLOR_INDIGO));
            safeSetText(relaySubtext, "ФІЗИЧНЕ РЕЛЕ ВІДКЛЮЧЕНО (ЛОГУВАННЯ)");
        } else if (mainApp.getRelayController().isConnected()) {
            safeSetText(relayStatusLabel, "Підключено");
            relayStatusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SUCCESS + ";");
            relayIndicator.setFill(Color.web(COLOR_SUCCESS));
            safeSetText(relaySubtext, mainApp.getRelayController().getConnectionDetails().toUpperCase());
        } else {
            safeSetText(relayStatusLabel, "Немає зв'язку");
            relayStatusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_DANGER + ";");
            relayIndicator.setFill(Color.web(COLOR_DANGER));
            safeSetText(relaySubtext, "ПЕРЕВІРТЕ ПІДКЛЮЧЕННЯ USB КАБЕЛЮ");
        }
        
        String countdown = "--:--:--";
        String nextType = "(на сьогодні все)";
        
        com.schoolbell.model.BellEntry nextEntry = mainApp.getSchedule().stream().filter(entry -> entry.time().isAfter(now)).findFirst().orElse(null);
        if (nextEntry != null) {
            java.time.Duration d = java.time.Duration.between(now, nextEntry.time());
            countdown = String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
            
            DaySchedule activeDs = mainApp.getInternalSchedules().stream().filter(ds -> ds.getName().equals(config.getSelectedScheduleName())).findFirst().orElse(null);
            if (activeDs != null) {
                for (int i = 0; i < activeDs.getLessons().size(); i++) {
                    DaySchedule.LessonInfo li = activeDs.getLessons().get(i);
                    if (now.isBefore(li.start)) {
                        nextType = (i + 1) + " урок (початок о " + li.start + ")"; break;
                    } else if (now.isAfter(li.start) && now.isBefore(li.end)) {
                        nextType = (i + 1) + " урок (кінець о " + li.end + ")"; break;
                    } else if (i < activeDs.getLessons().size() - 1) {
                        DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(i + 1);
                        if (now.isAfter(li.end) && now.isBefore(nextLi.start)) {
                            nextType = "Перерва (початок " + (i + 2) + " уроку о " + nextLi.start + ")"; break;
                        }
                    }
                }
            }
        }
        
        safeSetText(countdownLabel, countdown);
        safeSetText(nextBellTypeLabel, nextType);
        
        if (mediaValue != null) {
            String currentTrack = mainApp.getAudioService().getCurrentPlayingTrack();
            if (currentTrack != null) {
                safeSetText(mediaValue, "ЗАРАЗ: " + currentTrack.toUpperCase());
                applyInfoValueStyle(mediaValue, COLOR_TANGERINE);
                if (stopMediaBtn != null) {
                    stopMediaBtn.setVisible(true);
                    stopMediaBtn.setManaged(true);
                }
            } else {
                com.schoolbell.model.MediaEvent nextEvent = mainApp.getMediaSchedulerService().getNextEvent();
                if (nextEvent != null) {
                    safeSetText(mediaValue, nextEvent.time() + " — " + nextEvent.name().toUpperCase());
                } else {
                    safeSetText(mediaValue, "ПОДІЙ НЕ ЗАПЛАНОВАНО");
                }
                applyInfoValueStyle(mediaValue, COLOR_NAVY);
                if (stopMediaBtn != null) {
                    stopMediaBtn.setVisible(false);
                    stopMediaBtn.setManaged(false);
                }
            }
        }

        updateActionButtons();
        updateDashboardComponents(now);
    }

    private void updateActionButtons() {
        String alert = signalService.getCurrentAlertType();
        boolean inProgress = signalService.isActionInProgress();
        
        if ("AIR_RAID".equals(alert)) {
            updateActionButton(airRaidBtn, "ВІДБІЙ ТРИВОГИ", "Сигнал про завершення небезпеки", ICON_ALL_CLEAR, GRADIENT_SUCCESS);
            emergencyBtn.setDisable(true);
            airRaidBtn.setDisable(inProgress);
        } else if ("EMERGENCY".equals(alert)) {
            updateActionButton(emergencyBtn, "СКАСУВАТИ НС", "Повернення до штатного режиму", ICON_ALL_CLEAR, GRADIENT_SUCCESS);
            airRaidBtn.setDisable(true);
            emergencyBtn.setDisable(inProgress);
        } else if ("SILENCE".equals(alert)) {
            updateActionButton(airRaidBtn, "ХВИЛИНА МОВЧАННЯ", "Йде відтворення аудіо", ICON_CLOCK, GRADIENT_NEUTRAL);
            airRaidBtn.setDisable(true);
            emergencyBtn.setDisable(true);
        } else {
            updateActionButton(airRaidBtn, "ПОВІТРЯНА ТРИВОГА", "Активація режиму загальної небезпеки", ICON_AIR_RAID, GRADIENT_WARNING);
            updateActionButton(emergencyBtn, "ЕКСТРЕНА СИТУАЦІЯ", "Сигнал термінової евакуації закладу", ICON_LIFEBUOY, GRADIENT_DANGER);
            airRaidBtn.setDisable(inProgress);
            emergencyBtn.setDisable(inProgress);
        }
    }

    private void updateDashboardComponents(LocalTime now) {
        String scheduleName = config.getSelectedScheduleName();
        DaySchedule activeDs = mainApp.getInternalSchedules().stream()
                .filter(ds -> ds.getName().equals(scheduleName))
                .findFirst().orElse(null);
        
        if (activeDs == null) return;
        List<DaySchedule.LessonInfo> lessons = activeDs.getLessons();
        if (lessons.isEmpty()) return;

        int curLessonIdx = -1;
        boolean isBreak = false;
        boolean isBeforeDay = false;

        if (now.isBefore(lessons.get(0).start)) {
            isBeforeDay = true;
        } else {
            for (int i = 0; i < lessons.size(); i++) {
                DaySchedule.LessonInfo li = lessons.get(i);
                if (!now.isBefore(li.start) && now.isBefore(li.end)) {
                    curLessonIdx = i; isBreak = false; break;
                } else if (i < lessons.size() - 1) {
                    DaySchedule.LessonInfo nextLi = lessons.get(i + 1);
                    if (!now.isBefore(li.end) && now.isBefore(nextLi.start)) {
                        curLessonIdx = i; isBreak = true; break;
                    }
                }
            }
        }

        if (curLessonIdx != lastLessonIdx || isBreak != lastIsBreak || !Objects.equals(scheduleName, lastScheduleName)) {
            lastLessonIdx = curLessonIdx;
            lastIsBreak = isBreak;
            lastScheduleName = scheduleName;
        }

        boolean found = false;
        if (isBeforeDay) {
            DaySchedule.LessonInfo firstLi = lessons.get(0);
            safeSetText(curLessonNumLabel, "ПЕРЕД ЗАЙНЯТТЯМИ");
            safeSetText(curLessonStatusBadge, "ОЧІКУВАННЯ");
            curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_NEUTRAL + "; -fx-padding: 3 10; -fx-background-radius: 6;");
            safeSetText(curLessonTimeLabel, "Початок о " + firstLi.start);
            safeSetText(curLessonSubjectLabel, "Система готова до початку дня");
            safeSetProgress(curLessonProgress, 0);
            safeSetText(curLessonProgressText, "0%");

            safeSetText(nextLessonNumLabel, "1 УРОК");
            safeSetText(nextLessonStatusBadge, "ПОЧАТОК ДНЯ");
            safeSetText(nextLessonTimeLabel, firstLi.start + " — " + firstLi.end);
            safeSetText(nextLessonSubjectLabel, "Перше заняття сьогодні");
            found = true;
        } else if (curLessonIdx != -1) {
            DaySchedule.LessonInfo li = activeDs.getLessons().get(curLessonIdx);
            if (!isBreak) {
                safeSetText(curLessonNumLabel, (curLessonIdx + 1) + " УРОК");
                safeSetText(curLessonStatusBadge, "ТРИВАЄ");
                curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_PRIMARY + "; -fx-padding: 3 10; -fx-background-radius: 6;");
                safeSetText(curLessonTimeLabel, li.start + " — " + li.end);
                safeSetText(curLessonSubjectLabel, "Йде навчальний процес");
                
                long total = java.time.Duration.between(li.start, li.end).toSeconds();
                long elapsed = java.time.Duration.between(li.start, now).toSeconds();
                double p = Math.max(0, Math.min(1.0, (double) elapsed / total));
                safeSetProgress(curLessonProgress, p);
                safeSetText(curLessonProgressText, (int)(p * 100) + "% ЗАВЕРШЕНО");

                if (curLessonIdx < activeDs.getLessons().size() - 1) {
                    DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(curLessonIdx + 1);
                    safeSetText(nextLessonNumLabel, (curLessonIdx + 2) + " УРОК");
                    safeSetText(nextLessonStatusBadge, "НАСТУПНИЙ");
                    safeSetText(nextLessonTimeLabel, nextLi.start + " — " + nextLi.end);
                    safeSetText(nextLessonSubjectLabel, "Після перерви");
                } else {
                    safeSetText(nextLessonNumLabel, "--");
                    safeSetText(nextLessonStatusBadge, "КІНЕЦЬ");
                    safeSetText(nextLessonSubjectLabel, "Занять більше немає");
                }
                found = true;
            } else {
                DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(curLessonIdx + 1);
                safeSetText(curLessonNumLabel, "ПЕРЕРВА");
                safeSetText(curLessonStatusBadge, "ВІДПОЧИНОК");
                curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_WARNING + "; -fx-padding: 3 10; -fx-background-radius: 6;");
                safeSetText(curLessonTimeLabel, li.end + " — " + nextLi.start);
                safeSetText(curLessonSubjectLabel, "Час для відпочинку та підготовки");
                
                long total = java.time.Duration.between(li.end, nextLi.start).toSeconds();
                long elapsed = java.time.Duration.between(li.end, now).toSeconds();
                double p = Math.max(0, Math.min(1.0, (double) elapsed / total));
                safeSetProgress(curLessonProgress, p);
                safeSetText(curLessonProgressText, (int)(p * 100) + "% МИНУЛО");
                
                safeSetText(nextLessonNumLabel, (curLessonIdx + 2) + " УРОК");
                safeSetText(nextLessonStatusBadge, "ГОТУЙТЕСЯ");
                safeSetText(nextLessonTimeLabel, nextLi.start + " — " + nextLi.end);
                safeSetText(nextLessonSubjectLabel, "Наступний урок");
                found = true;
            }
        }
        
        if (!found) {
            safeSetText(curLessonNumLabel, "--");
            safeSetText(curLessonStatusBadge, "КІНЕЦЬ");
            curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_NEUTRAL + "; -fx-padding: 3 10; -fx-background-radius: 6;");
            safeSetText(curLessonSubjectLabel, "Навчальний день завершено");
            safeSetProgress(curLessonProgress, 0);
            safeSetText(curLessonProgressText, "0%");
            safeSetText(nextLessonNumLabel, "--");
            safeSetText(nextLessonSubjectLabel, "До завтра!");
        }
    }

    public Map<String, Object> getExtendedDashboardData(LocalTime now) {
        return dataModel.getExtendedDashboardData(now);
    }

    public void refreshActiveScheduleLabel() {
        if (activeScheduleValue != null) {
            safeSetText(activeScheduleValue, config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        }
        if (topActiveScheduleLabel != null) {
            safeSetText(topActiveScheduleLabel, config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        }
    }
}
