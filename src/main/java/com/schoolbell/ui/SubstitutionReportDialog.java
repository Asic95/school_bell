package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.ui.editor.SubstitutionReportService;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;

import static com.schoolbell.ui.UIStyles.*;

public class SubstitutionReportDialog extends BasePremiumDialog {
    private final SubstitutionReportService reportService;
    private final Locale ukLocale = Locale.of("uk", "UA");
    private final ComboBox<Month> monthPicker;
    private final Spinner<Integer> yearPicker;

    public SubstitutionReportDialog(MainApp mainApp, SubstitutionReportService reportService) {
        super(mainApp.getStage(),
                "ЗВІТНІСТЬ",
                "Генерація звіту замін",
                "Оберіть період для створення TXT-звіту про проведені заміни.",
                "ЗГЕНЕРУВАТИ TXT",
                500);

        this.reportService = reportService;

        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        labelCol.setMinWidth(170);
        labelCol.setPrefWidth(170);
        labelCol.setMaxWidth(170);
        
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        monthPicker = new ComboBox<>();
        monthPicker.getItems().addAll(Month.values());
        monthPicker.setValue(LocalDate.now().getMonth());
        monthPicker.setMaxWidth(Double.MAX_VALUE);
        monthPicker.setStyle(PREMIUM_SELECT_STYLE);
        monthPicker.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Month m) {
                if (m == null) return "";
                String name = m.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, ukLocale);
                return name.substring(0, 1).toUpperCase() + name.substring(1);
            }
            @Override
            public Month fromString(String s) { return null; }
        });

        grid.add(createLabel("МІСЯЦЬ"), 0, 0);
        grid.add(monthPicker, 1, 0);

        yearPicker = new Spinner<>(2024, 2030, LocalDate.now().getYear());
        yearPicker.setEditable(true);
        yearPicker.setMaxWidth(150);
        grid.add(createLabel("РІК"), 0, 1);
        grid.add(yearPicker, 1, 1);

        content.getChildren().add(grid);
    }

    @Override
    protected boolean onSave() {
        reportService.generateReport(monthPicker.getValue(), yearPicker.getValue());
        return true;
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(PREMIUM_LABEL_STYLE);
        return lbl;
    }
}
