package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.DaySchedule;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

import static com.schoolbell.ui.UIStyles.*;

/**
 * Premium dialog for quick schedule selection and preview.
 */
public class ScheduleQuickSelectorDialog extends BasePremiumDialog {
    private final MainApp mainApp;
    private final ComboBox<String> scheduleCombo;
    private final VBox overviewContainer;

    public ScheduleQuickSelectorDialog(MainApp mainApp) {
        super(mainApp.getStage(), "УПРАВЛІННЯ", "АКТИВНИЙ РОЗКЛАД",
                "Виберіть розклад зі списку для миттєвого застосування або перейдіть до редагування.", "ЗАСТОСУВАТИ", 580);
        this.mainApp = mainApp;

        scheduleCombo = new ComboBox<>();
        List<String> names = mainApp.getInternalSchedules().stream()
                .map(DaySchedule::getName)
                .collect(Collectors.toList());
        scheduleCombo.getItems().addAll(names);
        scheduleCombo.setValue(mainApp.getConfigService().getSelectedScheduleName());
        scheduleCombo.setMaxWidth(Double.MAX_VALUE);
        scheduleCombo.setStyle(PREMIUM_SELECT_STYLE);
        
        // Custom cell style for ComboBox
        scheduleCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-padding: 12 16; -fx-font-weight: 600; -fx-text-fill: " + COLOR_NAVY + "; -fx-background-radius: 12;");
                }
            }
        });

        overviewContainer = new VBox(10);
        overviewContainer.setPadding(new Insets(18, 22, 18, 22));
        overviewContainer.setStyle("-fx-background-color: " + COLOR_SURFACE_SKY + "; -fx-background-radius: 22; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-width: 1; -fx-border-radius: 22;");

        content.setSpacing(18);
        content.getChildren().addAll(
                ControlFactory.createLabeledField("ДОСТУПНІ РОЗКЛАДИ", scheduleCombo),
                new Label("ОГЛЯД ГОДИН"),
                overviewContainer
        );
        ((Label)content.getChildren().get(1)).setStyle(HEADER_STYLE);

        Button editBtn = ControlFactory.createSecondaryDialogButton("РЕДАГУВАТИ");
        editBtn.setGraphic(UIComponents.createSVGIcon(ICON_EDIT, Color.web(COLOR_SLATE), 18));
        editBtn.setGraphicTextGap(10);
        editBtn.setOnAction(e -> {
            close();
            mainApp.showEditorTab(0);
        });
        addLeftFooterButton(editBtn);

        scheduleCombo.setOnAction(e -> updateOverview());
        updateOverview();
    }

    private void updateOverview() {
        overviewContainer.getChildren().clear();
        String selected = scheduleCombo.getValue();
        if (selected == null) {
            VBox empty = ControlFactory.createEmptyState(ICON_INFO, "Розклад не вибрано", "Будь ласка, оберіть розклад зі списку вище для перегляду деталей.");
            empty.setPadding(new Insets(30));
            overviewContainer.getChildren().add(empty);
            return;
        }

        DaySchedule ds = mainApp.getInternalSchedules().stream()
                .filter(s -> s.getName().equals(selected))
                .findFirst().orElse(null);

        if (ds == null || ds.getLessons().stream().allMatch(l -> l.start == null)) {
            VBox empty = ControlFactory.createEmptyState(ICON_INFO, "Порожній розклад", "У цьому розкладі ще не налаштовано жодного навчального заняття.");
            empty.setPadding(new Insets(30));
            overviewContainer.getChildren().add(empty);
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(14);
        
        int row = 0;
        List<DaySchedule.LessonInfo> validLessons = ds.getLessons().stream()
                .filter(l -> l.start != null && l.end != null)
                .collect(Collectors.toList());

        for (int i = 0; i < validLessons.size(); i++) {
            DaySchedule.LessonInfo li = validLessons.get(i);

            Label num = new Label((i + 1) + " УРОК");
            num.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
            
            HBox timeAndBreak = new HBox(12);
            timeAndBreak.setAlignment(Pos.CENTER_LEFT);
            
            Label time = new Label(li.start + " — " + li.end);
            time.setStyle("-fx-font-weight: 800; -fx-text-fill: " + COLOR_NAVY + "; -fx-font-size: 16px;");
            timeAndBreak.getChildren().add(time);

            if (li.breakAfterMinutes > 0 && i < validLessons.size() - 1) {
                Label breakChip = new Label("+" + li.breakAfterMinutes + " хв перерва");
                breakChip.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_SLATE + "; -fx-background-color: " + COLOR_BORDER_SOFT + "; -fx-padding: 2 8; -fx-background-radius: 6;");
                timeAndBreak.getChildren().add(breakChip);
            }
            
            grid.add(num, 0, row);
            grid.add(timeAndBreak, 1, row);
            row++;
        }
        
        if (row == 0) {
            VBox empty = ControlFactory.createEmptyState(ICON_INFO, "Немає даних", "Години проведення занять не вказані.");
            empty.setPadding(new Insets(30));
            overviewContainer.getChildren().add(empty);
        } else {
            overviewContainer.getChildren().add(grid);
        }
    }

    @Override
    protected boolean onSave() {
        String selected = scheduleCombo.getValue();
        if (selected != null) {
            mainApp.getConfigService().setSelectedScheduleName(selected);
            mainApp.saveConfig();
            mainApp.reloadSchedule();
            mainApp.getDashboardView().refreshActiveScheduleLabel();
            ToastService.showSuccess("Розклад застосовано: " + selected);
            return true;
        }
        return false;
    }
}
