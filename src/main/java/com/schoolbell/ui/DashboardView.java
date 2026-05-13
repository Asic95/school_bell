package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.model.SchoolClass;
import com.schoolbell.model.SubstitutionEntry;
import com.schoolbell.service.AcademicService;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.SignalService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.scene.CacheHint;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class DashboardView {
    private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private static final String MODERN_PROGRESS_STYLE = 
        ".progress-bar { -fx-background-color: transparent; -fx-padding: 0; -fx-background-insets: 0; } " +
        ".progress-bar > .track { -fx-background-color: #f1f2f6; -fx-background-radius: 10; -fx-background-insets: 0; } " +
        ".progress-bar > .bar { -fx-background-color: linear-gradient(to right, " + COLOR_PRIMARY + ", " + COLOR_PURPLE + "); -fx-background-radius: 10; -fx-background-insets: 0; }";

    private final MainApp mainApp;
    private final ConfigService config;
    private final SignalService signalService;
    private final AcademicService academicService;

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
    
    private int lastLessonIdx = -2;
    private boolean lastIsBreak = false;
    private String lastScheduleName = "__INITIAL__";
    private final List<Animation> activeAnimations = new ArrayList<>();

    public DashboardView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.signalService = mainApp.getSignalService();
        this.academicService = mainApp.getAcademicService();
    }

    public Node build() {
        // Скидаємо стан для примусового оновлення таймлайну при кожній побудові UI
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
        statusRow.setAlignment(Pos.CENTER_LEFT); // Центрування по вертикалі всередині рядка
        
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
        heroCard.setPadding(new Insets(30));
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
        topActiveScheduleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-background-color: " + COLOR_BLUE_LIGHT + "; -fx-padding: 4 12; -fx-background-radius: 20;");
        heroHeader.getChildren().addAll(flowHeader, headerSpacer, topActiveScheduleLabel);

        HBox currentSessionInfo = new HBox(30);
        currentSessionInfo.setAlignment(Pos.CENTER_LEFT);
        
        VBox sessionIcon = new VBox(createSVGIcon(ICON_BOOK, Color.web(COLOR_PRIMARY), 48));
        sessionIcon.setAlignment(Pos.CENTER);
        sessionIcon.setPrefSize(90, 90);
        sessionIcon.setStyle("-fx-background-color: " + COLOR_BLUE_LIGHT + "; -fx-background-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(9, 132, 227, 0.2), 15, 0, 0, 5);");

        VBox sessionText = new VBox(5);
        curLessonNumLabel = new Label("--");
        curLessonNumLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        curLessonStatusBadge = new Label("ОЧІКУВАННЯ");
        curLessonStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_NEUTRAL + "; -fx-padding: 4 10; -fx-background-radius: 8;");
        HBox titleRow = new HBox(15, curLessonNumLabel, curLessonStatusBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        curLessonSubjectLabel = new Label("Завантаження даних...");
        curLessonSubjectLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        sessionText.getChildren().addAll(titleRow, curLessonSubjectLabel);
        
        Region sessionSpacer = new Region();
        HBox.setHgrow(sessionSpacer, Priority.ALWAYS);
        
        VBox countdownBox = new VBox(2);
        countdownBox.setAlignment(Pos.CENTER_RIGHT);
        Label countdownTitle = new Label("ДО НАСТУПНОГО ДЗВІНКА");
        countdownTitle.setStyle(HEADER_STYLE + "-fx-font-size: 9px;");
        countdownLabel = new Label("00:00:00");
        countdownLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + ";");
        nextBellTypeLabel = new Label("—");
        nextBellTypeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        countdownBox.getChildren().addAll(countdownTitle, countdownLabel, nextBellTypeLabel);
        
        currentSessionInfo.getChildren().addAll(sessionIcon, sessionText, sessionSpacer, countdownBox);

        VBox progressArea = new VBox(10);
        curLessonProgress = new ProgressBar(0);
        curLessonProgress.setMaxWidth(Double.MAX_VALUE);
        curLessonProgress.setPrefHeight(20); 
        curLessonProgress.getStylesheets().add("data:text/css," + MODERN_PROGRESS_STYLE.replace(" ", "%20"));
        
        HBox progressLabels = new HBox();
        curLessonTimeLabel = new Label("--:-- – --:--");
        curLessonTimeLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        curLessonProgressText = new Label("0%");
        curLessonProgressText.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + ";");
        progressLabels.getChildren().addAll(curLessonTimeLabel, pSpacer, curLessonProgressText);
        progressArea.getChildren().addAll(progressLabels, curLessonProgress);

        scheduleFlowContainer = new HBox(40); 
        scheduleFlowContainer.setAlignment(Pos.CENTER);
        scheduleFlowContainer.setPadding(new Insets(20, 0, 10, 0));
        
        heroCard.getChildren().addAll(heroHeader, currentSessionInfo, progressArea, scheduleFlowContainer);
        grid.add(heroCard, 0, 1);

        VBox nextEventCard = new VBox(20);
        nextEventCard.setPadding(new Insets(30));
        nextEventCard.setStyle(SOFT_CARD);
        nextEventCard.setCache(true);
        nextEventCard.setCacheHint(CacheHint.QUALITY);
        
        Label nextHeader = new Label("НАСТУПНА ПОДІЯ");
        nextHeader.setStyle(HEADER_STYLE);
        
        VBox nextContent = new VBox(15);
        nextContent.setAlignment(Pos.CENTER);
        
        VBox nextIconCircle = new VBox(createSVGIcon(ICON_CLOCK, Color.web(COLOR_SUCCESS), 40));
        nextIconCircle.setAlignment(Pos.CENTER);
        nextIconCircle.setPrefSize(80, 80);
        nextIconCircle.setStyle("-fx-background-color: " + COLOR_GREEN_LIGHT + "; -fx-background-radius: 50;");
        
        nextLessonNumLabel = new Label("--");
        nextLessonNumLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        
        VBox nextInfoText = new VBox(4);
        nextInfoText.setAlignment(Pos.CENTER);
        nextLessonSubjectLabel = new Label("Очікування...");
        nextLessonSubjectLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        nextLessonTimeLabel = new Label("--:-- – --:--");
        nextLessonTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        nextInfoText.getChildren().addAll(nextLessonSubjectLabel, nextLessonTimeLabel);
        
        nextLessonStatusBadge = new Label("ПЕРЕРВА");
        nextLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 4 12; -fx-background-radius: 20;");
        
        nextContent.getChildren().addAll(nextIconCircle, nextLessonNumLabel, nextInfoText, nextLessonStatusBadge);
        
        nextEventCard.getChildren().addAll(nextHeader, nextContent);
        grid.add(nextEventCard, 1, 1);

        HBox actionsBox = new HBox(25);
        airRaidBtn = createActionButton("ПОВІТРЯНА ТРИВОГА", "Активація режиму загальної небезпеки", ICON_AIR_RAID, GRADIENT_WARNING);
        airRaidBtn.setOnAction(e -> {
            if ("AIR_RAID".equals(signalService.getCurrentAlertType())) {
                // Миттєвий зворотній зв'язок в UI
                updateActionButton(airRaidBtn, "ПОВІТРЯНА ТРИВОГА", "Активація режиму загальної небезпеки", ICON_AIR_RAID, GRADIENT_WARNING);
                signalService.runAirRaidClearSignal();
            } else {
                // Миттєвий зворотній зв'язок в UI
                updateActionButton(airRaidBtn, "ВІДБІЙ ТРИВОГИ", "Сигнал про завершення небезпеки", ICON_ALL_CLEAR, GRADIENT_SUCCESS);
                signalService.runAirRaidSignal();
            }
        });
        HBox.setHgrow(airRaidBtn, Priority.ALWAYS);
        
        emergencyBtn = createActionButton("ЕКСТРЕНА СИТУАЦІЯ", "Сигнал термінової евакуації закладу", ICON_LIFEBUOY, GRADIENT_DANGER);
        emergencyBtn.setOnAction(e -> {
            if ("EMERGENCY".equals(signalService.getCurrentAlertType())) {
                // Миттєвий зворотній зв'язок в UI
                updateActionButton(emergencyBtn, "ЕКСТРЕНА СИТУАЦІЯ", "Сигнал термінової евакуації закладу", ICON_LIFEBUOY, GRADIENT_DANGER);
                signalService.runEmergencyClearSignal();
            } else {
                // Миттєвий зворотній зв'язок в UI
                updateActionButton(emergencyBtn, "СКАСУВАТИ НС", "Повернення до штатного режиму", ICON_ALL_CLEAR, GRADIENT_SUCCESS);
                signalService.runEmergencySignal();
            }
        });
        HBox.setHgrow(emergencyBtn, Priority.ALWAYS);
        
        actionsBox.getChildren().addAll(airRaidBtn, emergencyBtn);
        
        VBox quickActionsCard = new VBox(15, new Label("ШВИДКІ ДІЇ"), actionsBox);
        ((Label)quickActionsCard.getChildren().get(0)).setStyle(HEADER_STYLE);
        quickActionsCard.setPadding(new Insets(25));
        quickActionsCard.setStyle(SOFT_CARD);
        quickActionsCard.setCache(true);
        quickActionsCard.setCacheHint(CacheHint.QUALITY);
        grid.add(quickActionsCard, 0, 2, 2, 1);

        HBox infoRow = new HBox(20);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        
        activeScheduleValue = new Label(config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        VBox schCard = createSmallInfoCard("АКТИВНИЙ РОЗКЛАД", activeScheduleValue, "Змінити", () -> mainApp.showEditorTab(0), ICON_CALENDAR, COLOR_BLUE_LIGHT, COLOR_PRIMARY, false, 0, null);
        
        Label volValue = new Label("Гучність: " + config.getSystemVolume() + "%");
        VBox volCard = createSmallInfoCard("ВІДТВОРЕННЯ ЗВУКУ", volValue, null, null, ICON_VOLUME, COLOR_GREEN_LIGHT, COLOR_SUCCESS, true, config.getSystemVolume(), (newValue) -> {
            config.setSystemVolume(newValue);
            mainApp.getAudioService().setVolume(newValue);
            mainApp.saveConfig();
        });
        
        VBox brCard = createSmallInfoCard("ТРАНСЛЯЦІЯ ДАШБОРДУ", new Label(config.isBroadcastEnabled() ? "Увімкнено" : "Вимкнено"), "Відкрити в браузері", () -> mainApp.getHostServices().showDocument("http://localhost:" + (config.getBroadcastPort())), ICON_MONITOR, COLOR_PURPLE_LIGHT, "#6c5ce7", false, 0, null);
        
        infoRow.getChildren().addAll(schCard, volCard, brCard);
        grid.add(infoRow, 0, 3, 2, 1);

        ScrollPane mainScroll = new ScrollPane(grid);
        mainScroll.setFitToWidth(true); mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        update(LocalTime.now());
        return mainScroll;
    }

    public void update(LocalTime now) {
        currentTimeLabel.setText(now.format(HH_MM_SS));
        
        if (mainApp.getRelayController().isConnected()) {
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
        if ("AIR_RAID".equals(alert)) {
            updateActionButton(airRaidBtn, "ВІДБІЙ ТРИВОГИ", "Сигнал про завершення небезпеки", ICON_ALL_CLEAR, GRADIENT_SUCCESS);
            emergencyBtn.setDisable(true);
        } else if ("EMERGENCY".equals(alert)) {
            updateActionButton(emergencyBtn, "СКАСУВАТИ НС", "Повернення до штатного режиму", ICON_ALL_CLEAR, GRADIENT_SUCCESS);
            airRaidBtn.setDisable(true);
        } else if ("SILENCE".equals(alert)) {
            updateActionButton(airRaidBtn, "ХВИЛИНА МОВЧАННЯ", "Йде відтворення аудіо", ICON_CLOCK, GRADIENT_NEUTRAL);
            airRaidBtn.setDisable(true);
            emergencyBtn.setDisable(true);
        } else {
            updateActionButton(airRaidBtn, "ПОВІТРЯНА ТРИВОГА", "Активація режиму загальної небезпеки", ICON_AIR_RAID, GRADIENT_WARNING);
            updateActionButton(emergencyBtn, "ЕКСТРЕНА СИТУАЦІЯ", "Сигнал термінової евакуації закладу", ICON_LIFEBUOY, GRADIENT_DANGER);
            airRaidBtn.setDisable(false);
            emergencyBtn.setDisable(false);
        }
    }

    private void updateDashboardComponents(LocalTime now) {
        String scheduleName = config.getSelectedScheduleName();
        DaySchedule activeDs = mainApp.getInternalSchedules().stream()
                .filter(ds -> ds.getName().equals(scheduleName))
                .findFirst().orElse(null);
        
        if (activeDs == null || scheduleFlowContainer == null) return;

        int curLessonIdx = -1;
        boolean isBreak = false;
        for (int i = 0; i < activeDs.getLessons().size(); i++) {
            DaySchedule.LessonInfo li = activeDs.getLessons().get(i);
            if (!now.isBefore(li.start) && now.isBefore(li.end)) {
                curLessonIdx = i; isBreak = false; break;
            } else if (i < activeDs.getLessons().size() - 1) {
                DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(i + 1);
                if (!now.isBefore(li.end) && now.isBefore(nextLi.start)) {
                    curLessonIdx = i; isBreak = true; break;
                }
            }
        }

        if (curLessonIdx != lastLessonIdx || isBreak != lastIsBreak || !Objects.equals(scheduleName, lastScheduleName)) {
            rebuildTimeline(activeDs, curLessonIdx, isBreak, now);
            lastLessonIdx = curLessonIdx;
            lastIsBreak = isBreak;
            lastScheduleName = scheduleName;
        }

        boolean found = false;
        if (curLessonIdx != -1) {
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

    private void rebuildTimeline(DaySchedule ds, int curIdx, boolean isBreak, LocalTime now) {
        activeAnimations.forEach(Animation::stop);
        activeAnimations.clear();
        scheduleFlowContainer.getChildren().clear();
        
        if (curIdx == -1) {
            if (now.isBefore(ds.getLessons().get(0).start)) {
                for (int i = 0; i < Math.min(3, ds.getLessons().size()); i++) {
                    addTimelinePoint(i + 1, ds.getLessons().get(i), "upcoming");
                }
            } else {
                int total = ds.getLessons().size();
                for (int i = Math.max(0, total - 3); i < total; i++) {
                    addTimelinePoint(i + 1, ds.getLessons().get(i), "completed");
                }
            }
        } else if (isBreak) {
            addTimelinePoint(curIdx + 1, ds.getLessons().get(curIdx), "completed");
            addTimelineBreakPoint();
            addTimelinePoint(curIdx + 2, ds.getLessons().get(curIdx + 1), "upcoming");
        } else {
            if (curIdx > 0) addTimelinePoint(curIdx, ds.getLessons().get(curIdx - 1), "completed");
            addTimelinePoint(curIdx + 1, ds.getLessons().get(curIdx), "active");
            if (curIdx < ds.getLessons().size() - 1) addTimelinePoint(curIdx + 2, ds.getLessons().get(curIdx + 1), "upcoming");
        }
    }

    private void addTimelinePoint(int number, DaySchedule.LessonInfo li, String status) {
        VBox node = new VBox(8);
        node.setAlignment(Pos.CENTER);
        node.setMinWidth(120);
        
        StackPane box = new StackPane();
        box.setPrefSize(44, 44);
        box.setCache(true);
        box.setCacheHint(CacheHint.SCALE);
        
        Label num = new Label(String.valueOf(number));
        num.setStyle("-fx-font-weight: 900; -fx-font-size: 15px;");
        
        if ("active".equals(status)) {
            box.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-background-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(9, 132, 227, 0.4), 12, 0, 0, 0);");
            num.setTextFill(Color.WHITE);
            
            ScaleTransition st = new ScaleTransition(Duration.millis(1000), box);
            st.setFromX(1.0); st.setFromY(1.0);
            st.setToX(1.1); st.setToY(1.1);
            st.setCycleCount(Animation.INDEFINITE);
            st.setAutoReverse(true);
            st.play();
            activeAnimations.add(st);
        } else if ("completed".equals(status)) {
            box.setStyle("-fx-background-color: " + COLOR_GREEN_LIGHT + "; -fx-background-radius: 14; -fx-border-color: " + COLOR_SUCCESS + "; -fx-border-width: 1.5; -fx-border-radius: 14;");
            num.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #16a085;"); 
            Node check = createSVGIcon(ICON_CHECK, Color.web(COLOR_SUCCESS), 14);
            StackPane.setAlignment(check, Pos.TOP_RIGHT);
            StackPane.setMargin(check, new Insets(5, 5, 0, 0));
            box.getChildren().add(check);
        } else {
            box.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #dfe6e9; -fx-border-width: 1.5; -fx-border-radius: 14; -fx-opacity: 0.8;");
            num.setTextFill(Color.web(COLOR_TEXT_DIM));
        }
        
        box.getChildren().add(num);
        
        Label time = new Label(li.start.toString());
        time.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        
        Label label = new Label("УРОК");
        label.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #b2bec3; -fx-letter-spacing: 1px;");
        
        node.getChildren().addAll(label, box, time);
        scheduleFlowContainer.getChildren().add(node);
    }

    private void addTimelineBreakPoint() {
        VBox node = new VBox(8);
        node.setAlignment(Pos.CENTER);
        node.setMinWidth(120);
        
        StackPane box = new StackPane();
        box.setPrefSize(44, 44);
        box.setCache(true);
        box.setCacheHint(CacheHint.SCALE);
        box.setStyle("-fx-background-color: " + COLOR_WARNING + "; -fx-background-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(253, 203, 110, 0.3), 10, 0, 0, 0);");
        
        Node icon = createSVGIcon(ICON_CLOCK, Color.WHITE, 20);
        box.getChildren().add(icon);
        
        ScaleTransition st = new ScaleTransition(Duration.millis(1000), box);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.1); st.setToY(1.1);
        st.setCycleCount(Animation.INDEFINITE);
        st.setAutoReverse(true);
        st.play();
        activeAnimations.add(st);
        
        Label time = new Label("ПЕРЕРВА");
        time.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_WARNING + ";");
        
        Label label = new Label("ЗАРАЗ");
        label.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_WARNING + "; -fx-letter-spacing: 1px;");
        
        node.getChildren().addAll(label, box, time);
        scheduleFlowContainer.getChildren().add(node);
    }

    public Map<String, Object> getExtendedDashboardData(LocalTime now) {
        String countdown = countdownLabel.getText();
        Map<String, Object> data = new HashMap<>();
        data.put("schoolName", config.getSchoolName());
        data.put("cityName", config.getCityName());
        data.put("announcement", config.getAnnouncementText());
        
        String alert = signalService.getCurrentAlertType();
        if ("AIR_RAID".equals(alert) && !config.isVisualAirRaidEnabled()) alert = "NONE";
        if ("EMERGENCY".equals(alert) && !config.isVisualEmergencyEnabled()) alert = "NONE";
        if ("SILENCE".equals(alert) && !config.isVisualSilenceEnabled()) alert = "NONE";
        data.put("alertType", alert);

        data.put("countdown", countdown);
        data.put("scheduleName", config.getSelectedScheduleName());

        DaySchedule activeDs = mainApp.getInternalSchedules().stream().filter(ds -> ds.getName().equals(config.getSelectedScheduleName())).findFirst().orElse(null);
        if (activeDs == null) return data;

        List<Map<String, Object>> stages = new ArrayList<>();
        int currentStageIndex = -1;
        for (int i = 0; i < activeDs.getLessons().size(); i++) {
            DaySchedule.LessonInfo li = activeDs.getLessons().get(i);
            Map<String, Object> lessonStage = new HashMap<>();
            lessonStage.put("type", "LESSON");
            lessonStage.put("number", i + 1);
            lessonStage.put("title", (i + 1) + " урок");
            lessonStage.put("start", li.start != null ? li.start.toString() : "--:--");
            lessonStage.put("end", li.end != null ? li.end.toString() : "--:--");
            
            if (li.start != null && li.end != null) {
                if (now.isBefore(li.start)) {
                    lessonStage.put("status", "upcoming");
                } else if (now.isAfter(li.end)) {
                    lessonStage.put("status", "completed");
                } else {
                    lessonStage.put("status", "active");
                    currentStageIndex = stages.size();
                    long total = java.time.Duration.between(li.start, li.end).toSeconds();
                    long elapsed = java.time.Duration.between(li.start, now).toSeconds();
                    lessonStage.put("progress", (elapsed * 100.0) / total);
                }
            } else {
                lessonStage.put("status", "upcoming");
            }
            stages.add(lessonStage);

            if (i < activeDs.getLessons().size() - 1) {
                DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(i + 1);
                if (li.end != null && nextLi.start != null) {
                    Map<String, Object> breakStage = new HashMap<>();
                    breakStage.put("type", "BREAK");
                    breakStage.put("title", "Перерва");
                    breakStage.put("start", li.end.toString());
                    breakStage.put("end", nextLi.start.toString());
                    
                    if (now.isBefore(li.end)) {
                        breakStage.put("status", "upcoming");
                    } else if (now.isAfter(nextLi.start)) {
                        breakStage.put("status", "completed");
                    } else {
                        breakStage.put("status", "active");
                        currentStageIndex = stages.size();
                        long total = java.time.Duration.between(li.end, nextLi.start).toSeconds();
                        long elapsed = java.time.Duration.between(li.end, now).toSeconds();
                        breakStage.put("progress", (elapsed * 100.0) / total);
                    }
                    stages.add(breakStage);
                }
            }
        }
        data.put("stages", stages);
        data.put("currentStageIndex", currentStageIndex);

        List<Map<String, Object>> classStatuses = new ArrayList<>();
        int currentLessonNum = -1;
        for (int i = 0; i < activeDs.getLessons().size(); i++) {
            DaySchedule.LessonInfo li = activeDs.getLessons().get(i);
            if (li.start != null && li.end != null && !now.isBefore(li.start) && !now.isAfter(li.end)) {
                currentLessonNum = i + 1; break;
            }
        }

        if (currentLessonNum != -1) {
            java.time.LocalDate today = java.time.LocalDate.now();
            int dayOfWeek = today.getDayOfWeek().getValue();
            List<SubstitutionEntry> subs = academicService.getSubstitutionsForDate(today);
            
            for (SchoolClass sc : mainApp.getClassCache()) {
                Map<String, Object> cs = new HashMap<>();
                cs.put("className", sc.name());
                cs.put("lessonNumber", currentLessonNum);
                cs.put("statusClass", "current");

                final int lNum = currentLessonNum;
                SubstitutionEntry sub = subs.stream()
                        .filter(s -> s.classId() == sc.id() && s.lessonNumber() == lNum)
                        .findFirst().orElse(null);
                
                if (sub != null) {
                    cs.put("isReplacement", true);
                    cs.put("subject", mainApp.getSubjectName(sub.subjectId()));
                    cs.put("teacher", mainApp.getTeacherName(sub.teacherId()));
                    cs.put("room", mainApp.getClassroomName(sub.classroomId()));
                    
                    // Find original teacher for the replacement view
                    List<com.schoolbell.model.ScheduleEntry> sched = academicService.getScheduleForClass(sc.id());
                    sched.stream()
                        .filter(e -> e.dayOfWeek() == dayOfWeek && e.lessonNumber() == lNum)
                        .findFirst()
                        .ifPresent(orig -> cs.put("originalTeacher", mainApp.getTeacherName(orig.teacherId())));
                    
                    if (!cs.containsKey("originalTeacher")) cs.put("originalTeacher", "—");
                } else {
                    List<com.schoolbell.model.ScheduleEntry> sched = academicService.getScheduleForClass(sc.id());
                    com.schoolbell.model.ScheduleEntry entry = sched.stream()
                            .filter(e -> e.dayOfWeek() == dayOfWeek && e.lessonNumber() == lNum)
                            .findFirst().orElse(null);
                    
                    if (entry != null) {
                        cs.put("subject", mainApp.getSubjectName(entry.subjectId()));
                        cs.put("teacher", mainApp.getTeacherName(entry.teacherId()));
                        cs.put("room", mainApp.getClassroomName(entry.classroomId()));
                    } else {
                        cs.put("subject", "—");
                        cs.put("teacher", "—");
                        cs.put("room", "—");
                    }
                }
                classStatuses.add(cs);
            }
        }
        data.put("classStatuses", classStatuses);
        data.put("rows", classStatuses); // dashboard.html expects 'rows'

        if (currentLessonNum != -1 && !classStatuses.isEmpty()) {
            Map<String, Object> firstClass = classStatuses.get(0);
            DaySchedule.LessonInfo li = activeDs.getLessons().get(currentLessonNum - 1);
            Map<String, Object> cl = new HashMap<>();
            cl.put("number", currentLessonNum);
            cl.put("subject", firstClass.get("subject"));
            cl.put("teacher", firstClass.get("teacher"));
            cl.put("room", firstClass.get("room"));
            cl.put("className", firstClass.get("className"));
            cl.put("start", li.start.toString());
            cl.put("end", li.end.toString());
            data.put("currentLesson", cl);
        }

        return data;
    }

    public void clearFlow() {
        if (scheduleFlowContainer != null) scheduleFlowContainer.getChildren().clear();
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
