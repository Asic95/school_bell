package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.BellEntry;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.service.AcademicService;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.SignalService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.time.LocalTime;
import java.util.Map;

import static com.schoolbell.ui.UIStyles.DEPTH_1;

public class DashboardView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final SignalService signalService;
    private final AcademicService academicService;
    private final DashboardDataModel dataModel;

    private DashboardTimeCard timeCard;
    private DashboardRelayCard relayCard;
    private DashboardHeroCard heroCard;
    private DashboardNextEventCard nextEventCard;
    private DashboardQuickActionsCard quickActionsCard;
    private DashboardInfoRow infoRow;

    private static final double RESPONSIVE_THRESHOLD = 1350;
    private boolean isSmallMode = false;

    public DashboardView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.signalService = mainApp.getSignalService();
        this.academicService = mainApp.getAcademicService();
        this.dataModel = new DashboardDataModel(mainApp);
    }

    public Node build() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(25));
        grid.setStyle(DEPTH_1);

        timeCard = new DashboardTimeCard();
        relayCard = new DashboardRelayCard();
        heroCard = new DashboardHeroCard(config.getSelectedScheduleName());
        nextEventCard = new DashboardNextEventCard();
        quickActionsCard = new DashboardQuickActionsCard(signalService);
        infoRow = new DashboardInfoRow(mainApp);

        grid.add(timeCard, 0, 0);
        grid.add(relayCard, 1, 0);
        grid.add(heroCard, 0, 1);
        grid.add(nextEventCard, 1, 1);
        grid.add(quickActionsCard, 0, 2, 2, 1);
        grid.add(infoRow, 0, 3, 2, 1);

        // Initial setup for column constraints to avoid flicker
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(55);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(45);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.widthProperty().addListener((obs, oldW, newW) -> {
            boolean small = newW.doubleValue() < RESPONSIVE_THRESHOLD;
            if (small != isSmallMode || oldW.doubleValue() == 0) {
                applyResponsiveLayout(grid, small);
            }
        });

        ScrollPane mainScroll = new ScrollPane(grid);
        mainScroll.setFitToWidth(true);
        mainScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        update(LocalTime.now());
        return mainScroll;
    }

    private void applyResponsiveLayout(GridPane grid, boolean small) {
        this.isSmallMode = small;
        grid.getColumnConstraints().clear();
        
        if (small) {
            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(50);
            ColumnConstraints c2 = new ColumnConstraints();
            c2.setPercentWidth(50);
            grid.getColumnConstraints().addAll(c1, c2);

            GridPane.setConstraints(heroCard, 0, 1, 2, 1);
            GridPane.setConstraints(nextEventCard, 0, 2, 2, 1);
            GridPane.setConstraints(quickActionsCard, 0, 3, 2, 1);
            GridPane.setConstraints(infoRow, 0, 4, 2, 1);
        } else {
            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(55);
            ColumnConstraints c2 = new ColumnConstraints();
            c2.setPercentWidth(45);
            grid.getColumnConstraints().addAll(c1, c2);

            GridPane.setConstraints(heroCard, 0, 1, 1, 1);
            GridPane.setConstraints(nextEventCard, 1, 1, 1, 1);
            GridPane.setConstraints(quickActionsCard, 0, 2, 2, 1);
            GridPane.setConstraints(infoRow, 0, 3, 2, 1);
        }
    }

    public void update(LocalTime now) {
        if (timeCard != null) timeCard.update(now);
        if (relayCard != null) relayCard.update(mainApp, config);
        
        String countdown = "--:--:--";
        String nextType = "(на сьогодні все)";

        BellEntry nextEntry = mainApp.getSchedule().stream().filter(entry -> entry.time().isAfter(now)).findFirst().orElse(null);
        if (nextEntry != null) {
            java.time.Duration d = java.time.Duration.between(now, nextEntry.time());
            countdown = String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());

            DaySchedule activeDs = getActiveSchedule();
            if (activeDs != null) {
                for (int i = 0; i < activeDs.getLessons().size(); i++) {
                    DaySchedule.LessonInfo li = activeDs.getLessons().get(i);
                    if (now.isBefore(li.start)) {
                        nextType = (i + 1) + " урок (початок о " + li.start + ")";
                        break;
                    } else if (now.isAfter(li.start) && now.isBefore(li.end)) {
                        nextType = (i + 1) + " урок (кінець о " + li.end + ")";
                        break;
                    } else if (i < activeDs.getLessons().size() - 1) {
                        DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(i + 1);
                        if (now.isAfter(li.end) && now.isBefore(nextLi.start)) {
                            nextType = "Перерва (початок " + (i + 2) + " уроку о " + nextLi.start + ")";
                            break;
                        }
                    }
                }
            }
        }

        DaySchedule activeDs = getActiveSchedule();
        if (heroCard != null) heroCard.update(now, activeDs, countdown, nextType);
        if (nextEventCard != null) nextEventCard.update(now, activeDs);
        if (quickActionsCard != null) quickActionsCard.update(signalService);
        if (infoRow != null) {
            infoRow.updateMedia();
            infoRow.updateVolume(config.getSystemVolume());
        }
    }

    private DaySchedule getActiveSchedule() {
        String scheduleName = config.getSelectedScheduleName();
        return mainApp.getInternalSchedules().stream()
                .filter(ds -> ds.getName().equals(scheduleName))
                .findFirst().orElse(null);
    }

    public Map<String, Object> getExtendedDashboardData(LocalTime now) {
        return dataModel.getExtendedDashboardData(now);
    }

    public void refreshActiveScheduleLabel() {
        String name = config.getSelectedScheduleName();
        if (heroCard != null) heroCard.refreshScheduleName(name);
        if (infoRow != null) infoRow.updateSchedule(name);
    }
}
