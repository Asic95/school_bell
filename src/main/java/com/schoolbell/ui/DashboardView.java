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
    private DashboardTimeline timeline;

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

    private Label activeScheduleValue;
    private Label topActiveScheduleLabel;
    private HBox scheduleFlowContainer;

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
        grid.setHgap(25);
        grid.setVgap(25);
        grid.setPadding(new Insets(30));
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
        timeCard.setCache(true);
        timeCard.setCacheHint(CacheHint.SPEED);
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
        relayCard.setCache(true);
        relayCard.setCacheHint(CacheHint.SPEED);
        grid.add(relayCard, 1, 0);

        // --- MIDDLE ROW ---
        VBox heroCard = new VBox(25);
        heroCard.setPadding(new Insets(35));
        heroCard.setStyle(SOFT_CARD);
        heroCard.setCache(true);
        heroCard.setCacheHint(CacheHint.SPEED);
        
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
        sessionIcon.setPrefSize(94, 94);
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
        curLessonProgress.setPrefHeight(22); 
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

        scheduleFlowContainer = new HBox(40); 
        scheduleFlowContainer.setAlignment(Pos.CENTER);
        scheduleFlowContainer.setPadding(new Insets(20, 0, 10, 0));
        timeline = new DashboardTimeline(scheduleFlowContainer);
        
        heroCard.getChildren().addAll(heroHeader, currentSessionInfo, progressArea, scheduleFlowContainer);
        grid.add(heroCard, 0, 1);

        VBox nextEventCard = new VBox(25);
        nextEventCard.setPadding(new Insets(35));
        nextEventCard.setStyle(SOFT_CARD);
        nextEventCard.setCache(true);
        nextEventCard.setCacheHint(CacheHint.SPEED);
        
        Label nextHeader = new Label("НАСТУПНА ПОДІЯ");
        nextHeader.setStyle(HEADER_STYLE);
        
        VBox nextContent = new VBox(18);
        nextContent.setAlignment(Pos.CENTER);
        
        VBox nextIconCircle = new VBox(createSVGIcon(ICON_CLOCK, Color.web(COLOR_SUCCESS), 44));
        nextIconCircle.setAlignment(Pos.CENTER);
        nextIconCircle.setPrefSize(84, 84);
        nextIconCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, " + COLOR_SURFACE_GLASS_START + ", " + COLOR_SURFACE_GLASS_END + "); -fx-background-radius: 22;");
        
        nextLessonNumLabel = new Label("--");
        nextLessonNumLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        
        VBox nextInfoText = new VBox(4);
        nextInfoText.setAlignment(Pos.CENTER);
        nextLessonSubjectLabel = new Label("Очікування...");
        nextLessonSubjectLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        nextLessonTimeLabel = new Label("--:-- – --:--");
        nextLessonTimeLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_SLATE + ";");
        nextInfoText.getChildren().addAll(nextLessonSubjectLabel, nextLessonTimeLabel);
        
        nextLessonStatusBadge = new Label("ПЕРЕРВА");
        nextLessonStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 5 15; -fx-background-radius: 20;");
        
        nextContent.getChildren().addAll(nextIconCircle, nextLessonNumLabel, nextInfoText, nextLessonStatusBadge);
        
        nextEventCard.getChildren().addAll(nextHeader, nextContent);
        grid.add(nextEventCard, 1, 1);

        HBox actionsBox = new HBox(25);
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
        quickActionsCard.setPadding(new Insets(30));
        quickActionsCard.setStyle(SOFT_CARD);
        quickActionsCard.setCache(true);
        quickActionsCard.setCacheHint(CacheHint.SPEED);
        grid.add(quickActionsCard, 0, 2, 2, 1);

        HBox infoRow = new HBox(20);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        
        activeScheduleValue = new Label(config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        VBox schCard = createSmallInfoCard("АКТИВНИЙ РОЗКЛАД", activeScheduleValue, "Змінити", () -> mainApp.showEditorTab(0), ICON_CALENDAR, COLOR_BLUE_LIGHT, COLOR_PRIMARY, false, 0, null);
        
        currentVolumeValue = config.getSystemVolume();
        volStatusLabel = new Label(currentVolumeValue + "%");
        volStatusLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        
        volumePresetBox = new HBox(5);
        volumePresetBox.setAlignment(Pos.CENTER_LEFT);
        int[] presets = {0, 25, 50, 75, 100};
        for (int p : presets) {
            Button pb = new Button(p == 0 ? "0" : p + "");
            pb.setPrefSize(35, 30);
            pb.setUserData(p);
            pb.setOnAction(e -> {
                currentVolumeValue = p;
                volStatusLabel.setText(p + "%");
                updateVolumeStyle();
                config.setSystemVolume(p);
                mainApp.getAudioService().setVolume(p);
                mainApp.getSystemService().setWindowsSystemVolume(p);
                mainApp.saveConfig();
            });
            volumePresetBox.getChildren().add(pb);
        }
        updateVolumeStyle();

        VBox volContent = new VBox(5, volStatusLabel, volumePresetBox);
        VBox volCard = createSmallInfoCard("СИСТЕМНА ГУЧНІСТЬ", volContent, null, null, ICON_VOLUME, COLOR_GREEN_LIGHT, COLOR_SUCCESS, false, 0, null);
        
        VBox brCard = createSmallInfoCard("ТРАНСЛЯЦІЯ ДАШБОРДУ", new Label(config.isBroadcastEnabled() ? "Увімкнено" : "Вимкнено"), "Відкрити в браузері", () -> mainApp.getHostServices().showDocument("http://localhost:" + (config.getBroadcastPort())), ICON_MONITOR, COLOR_PURPLE_LIGHT, COLOR_INDIGO_SOFT, false, 0, null);
        
        infoRow.getChildren().addAll(schCard, volCard, brCard);
        grid.add(infoRow, 0, 3, 2, 1);

        ScrollPane mainScroll = new ScrollPane(grid);
        mainScroll.setFitToWidth(true); mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        update(LocalTime.now());
        return mainScroll;
    }

    private void updateVolumeStyle() {
        if (volumePresetBox == null) return;
        for (Node n : volumePresetBox.getChildren()) {
            if (n instanceof Button b) {
                int val = (int) b.getUserData();
                if (val == currentVolumeValue) {
                    b.setStyle("-fx-background-color: " + COLOR_SUCCESS + "; -fx-text-fill: white; -fx-font-weight: 900; -fx-background-radius: 8; -fx-font-size: 10px; -fx-padding: 0;");
                } else {
                    b.setStyle("-fx-background-color: " + COLOR_SURFACE_SUBTLE + "; -fx-text-fill: " + COLOR_NEUTRAL + "; -fx-font-weight: bold; -fx-background-radius: 8; -fx-font-size: 10px; -fx-padding: 0; -fx-cursor: hand;");
                }
            }
        }
    }

    public void update(LocalTime now) {
        currentTimeLabel.setText(now.format(HH_MM_SS));
        
        if (config.isSimulationMode()) {
            relayStatusLabel.setText("РЕЖИМ СИМУЛЯЦІЇ");
            relayStatusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_INDIGO + ";");
            relayIndicator.setFill(Color.web(COLOR_INDIGO));
            relaySubtext.setText("ФІЗИЧНЕ РЕЛЕ ВІДКЛЮЧЕНО (ЛОГУВАННЯ)");
        } else if (mainApp.getRelayController().isConnected()) {
            relayStatusLabel.setText("Підключено");
            relayStatusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SUCCESS + ";");
            relayIndicator.setFill(Color.web(COLOR_SUCCESS));
            relaySubtext.setText(mainApp.getRelayController().getConnectionDetails().toUpperCase());
        } else {
            relayStatusLabel.setText("Немає зв'язку");
            relayStatusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_DANGER + ";");
            relayIndicator.setFill(Color.web(COLOR_DANGER));
            relaySubtext.setText("ПЕРЕВІРТЕ ПІДКЛЮЧЕННЯ USB КАБЕЛЮ");
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
        
        countdownLabel.setText(countdown);
        nextBellTypeLabel.setText(nextType);
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
        
        if (activeDs == null || scheduleFlowContainer == null) return;
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
            timeline.rebuild(activeDs, curLessonIdx, isBreak, now);
            lastLessonIdx = curLessonIdx;
            lastIsBreak = isBreak;
            lastScheduleName = scheduleName;
        }

        boolean found = false;
        if (isBeforeDay) {
            DaySchedule.LessonInfo firstLi = lessons.get(0);
            curLessonNumLabel.setText("ПЕРЕД ЗАЙНЯТТЯМИ");
            curLessonStatusBadge.setText("ОЧІКУВАННЯ");
            curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_NEUTRAL + "; -fx-padding: 3 10; -fx-background-radius: 6;");
            curLessonTimeLabel.setText("Початок о " + firstLi.start);
            curLessonSubjectLabel.setText("Система готова до початку дня");
            curLessonProgress.setProgress(0);
            curLessonProgressText.setText("0%");

            nextLessonNumLabel.setText("1 УРОК");
            nextLessonStatusBadge.setText("ПОЧАТОК ДНЯ");
            nextLessonTimeLabel.setText(firstLi.start + " — " + firstLi.end);
            nextLessonSubjectLabel.setText("Перше заняття сьогодні");
            found = true;
        } else if (curLessonIdx != -1) {
            DaySchedule.LessonInfo li = activeDs.getLessons().get(curLessonIdx);
            if (!isBreak) {
                curLessonNumLabel.setText((curLessonIdx + 1) + " УРОК");
                curLessonStatusBadge.setText("ТРИВАЄ");
                curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_PRIMARY + "; -fx-padding: 3 10; -fx-background-radius: 6;");
                curLessonTimeLabel.setText(li.start + " — " + li.end);
                curLessonSubjectLabel.setText("Йде навчальний процес");
                
                long total = java.time.Duration.between(li.start, li.end).toSeconds();
                long elapsed = java.time.Duration.between(li.start, now).toSeconds();
                double p = Math.max(0, Math.min(1.0, (double) elapsed / total));
                curLessonProgress.setProgress(p);
                curLessonProgressText.setText((int)(p * 100) + "% ЗАВЕРШЕНО");

                if (curLessonIdx < activeDs.getLessons().size() - 1) {
                    DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(curLessonIdx + 1);
                    nextLessonNumLabel.setText((curLessonIdx + 2) + " УРОК");
                    nextLessonStatusBadge.setText("НАСТУПНИЙ");
                    nextLessonTimeLabel.setText(nextLi.start + " — " + nextLi.end);
                    nextLessonSubjectLabel.setText("Після перерви");
                } else {
                    nextLessonNumLabel.setText("--");
                    nextLessonStatusBadge.setText("КІНЕЦЬ");
                    nextLessonSubjectLabel.setText("Занять більше немає");
                }
                found = true;
            } else {
                DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(curLessonIdx + 1);
                curLessonNumLabel.setText("ПЕРЕРВА");
                curLessonStatusBadge.setText("ВІДПОЧИНОК");
                curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_WARNING + "; -fx-padding: 3 10; -fx-background-radius: 6;");
                curLessonTimeLabel.setText(li.end + " — " + nextLi.start);
                curLessonSubjectLabel.setText("Час для відпочинку та підготовки");
                
                long total = java.time.Duration.between(li.end, nextLi.start).toSeconds();
                long elapsed = java.time.Duration.between(li.end, now).toSeconds();
                double p = Math.max(0, Math.min(1.0, (double) elapsed / total));
                curLessonProgress.setProgress(p);
                curLessonProgressText.setText((int)(p * 100) + "% МИНУЛО");
                
                nextLessonNumLabel.setText((curLessonIdx + 2) + " УРОК");
                nextLessonStatusBadge.setText("ГОТУЙТЕСЯ");
                nextLessonTimeLabel.setText(nextLi.start + " — " + nextLi.end);
                nextLessonSubjectLabel.setText("Наступний урок");
                found = true;
            }
        }
        
        if (!found) {
            curLessonNumLabel.setText("--");
            curLessonStatusBadge.setText("КІНЕЦЬ");
            curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_NEUTRAL + "; -fx-padding: 3 10; -fx-background-radius: 6;");
            curLessonSubjectLabel.setText("Навчальний день завершено");
            curLessonProgress.setProgress(0);
            curLessonProgressText.setText("0%");
            nextLessonNumLabel.setText("--");
            nextLessonSubjectLabel.setText("До завтра!");
        }
    }

    public Map<String, Object> getExtendedDashboardData(LocalTime now) {
        return dataModel.getExtendedDashboardData(now);
    }

    public void clearFlow() {
        if (timeline != null) timeline.clear();
    }

    public void refreshActiveScheduleLabel() {
        if (activeScheduleValue != null) {
            activeScheduleValue.setText(config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        }
        if (topActiveScheduleLabel != null) {
            topActiveScheduleLabel.setText(config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        }
    }
}
