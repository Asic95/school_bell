package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.ui.editor.SubstitutionReportService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;

import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.UIStyles.*;

public class SubstitutionReportDialog extends Stage {
    private final SubstitutionReportService reportService;
    private final Locale ukLocale = Locale.of("uk", "UA");

    public SubstitutionReportDialog(MainApp mainApp, SubstitutionReportService reportService) {
        this.reportService = reportService;

        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        initOwner(mainApp.getStage());

        VBox root = new VBox(25);
        root.setPadding(new Insets(35));
        root.setStyle(SOFT_CARD);
        root.setPrefWidth(500);

        Label title = new Label("Генерація звіту замін");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        Label subtitle = new Label("Оберіть період для створення TXT-звіту про проведені заміни.");
        subtitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 500; -fx-text-fill: " + COLOR_SLATE + ";");

        VBox headerBox = new VBox(8, title, subtitle);

        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER_LEFT);
        
        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        labelCol.setPrefWidth(100);
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        ComboBox<Month> monthPicker = new ComboBox<>();
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

        Spinner<Integer> yearPicker = new Spinner<>(2024, 2030, LocalDate.now().getYear());
        yearPicker.setEditable(true);
        yearPicker.setMaxWidth(150);
        grid.add(createLabel("РІК"), 0, 1);
        grid.add(yearPicker, 1, 1);

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("СКАСУВАТИ");
        String cancelStyle = "-fx-background-color: white; -fx-text-fill: " + COLOR_SLATE + "; -fx-font-weight: 800; -fx-padding: 12 24; -fx-background-radius: 18; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 18; -fx-cursor: hand;";
        cancelBtn.setStyle(cancelStyle);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelStyle + "-fx-background-color: " + COLOR_SURFACE_SUBTLE + ";"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(cancelStyle));
        cancelBtn.setOnAction(e -> close());

        Button generateBtn = createPrimaryActionButton("ЗГЕНЕРУВАТИ TXT", ICON_SAVE);
        generateBtn.setStyle(PREMIUM_BTN_STYLE);
        generateBtn.setOnAction(e -> {
            reportService.generateReport(monthPicker.getValue(), yearPicker.getValue());
            close();
        });

        actions.getChildren().addAll(cancelBtn, generateBtn);
        root.getChildren().addAll(headerBox, grid, actions);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(
            "data:text/css," + MODERN_SPINNER_STYLE.replace(" ", "%20")
        );
        setScene(scene);
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE_LIGHT + "; -fx-letter-spacing: 1px;");
        return lbl;
    }
}
