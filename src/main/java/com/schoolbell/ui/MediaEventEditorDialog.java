package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.MediaEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;

import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class MediaEventEditorDialog extends Stage {
    private final MainApp mainApp;
    private final MediaEvent event;

    private TextField nameF;
    private ComboBox<String> typeC;
    private TextField pathF;
    private TextField timeF;
    private DatePicker dateP;
    private ComboBox<String> breakAnchorC;
    private TextField offsetF;

    public MediaEventEditorDialog(MainApp mainApp, MediaEvent event) {
        this.mainApp = mainApp;
        this.event = event;

        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        initOwner(mainApp.getStage());

        VBox root = new VBox(25);
        root.setPadding(new Insets(35));
        root.setStyle(SOFT_CARD + "-fx-background-radius: 32; -fx-border-radius: 32; -fx-border-width: 2; -fx-border-color: #e2e8f0;");
        root.setPrefWidth(650);

        Label title = new Label(event == null ? "Додати подію" : "Редагувати подію");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT + ";");
        Label subtitle = new Label(event == null ? "Налаштування нового автоматичного сповіщення." : event.name());
        subtitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #64748b;");

        VBox headerBox = new VBox(4, title, subtitle);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER_LEFT);
        
        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        labelCol.setPrefWidth(130);
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        nameF = createStyledField(event != null ? event.name() : "");
        nameF.setPromptText("Назва повідомлення");
        grid.add(createLabel("НАЗВА"), 0, 0);
        grid.add(nameF, 1, 0);

        typeC = new ComboBox<>();
        typeC.getItems().addAll("На перервах", "У конкретний час", "Разово");
        typeC.setValue(event != null ? switch (event.type()) {
            case "BREAKS" -> "На перервах";
            case "TIME" -> "У конкретний час";
            case "ONCE" -> "Разово";
            default -> "На перервах";
        } : "На перервах");
        typeC.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-border-radius: 12; -fx-background-color: white; -fx-border-color: #e2e8f0; -fx-padding: 8;");
        typeC.setMaxWidth(Double.MAX_VALUE);
        grid.add(createLabel("ТИП ТРИГЕРА"), 0, 1);
        grid.add(typeC, 1, 1);

        pathF = createStyledField(event != null ? event.path() : "");
        pathF.setEditable(false);
        pathF.setMaxWidth(Double.MAX_VALUE);

        Button browseFile = createPrimaryActionButton("Файл", ICON_MUSIC);
        Button browseFolder = createPrimaryActionButton("Папка", ICON_FOLDER);
        browseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо файли (MP3, WAV)", "*.mp3", "*.wav"));
            File f = fc.showOpenDialog(this);
            if (f != null) pathF.setText(f.getAbsolutePath());
        });
        browseFolder.setOnAction(e -> {
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            File f = dc.showDialog(this);
            if (f != null) pathF.setText(f.getAbsolutePath());
        });

        VBox pathWrapper = new VBox(10, pathF, new HBox(10, browseFile, browseFolder));
        grid.add(createLabel("ДЖЕРЕЛО ЗВУКУ"), 0, 2);
        grid.add(pathWrapper, 1, 2);

        timeF = createStyledField(event != null ? event.time() : "12:00");
        timeF.setPrefWidth(120);

        dateP = new DatePicker(event != null && event.date() != null && !event.date().isEmpty()
                ? java.time.LocalDate.parse(event.date())
                : java.time.LocalDate.now());
        dateP.setMaxWidth(200);

        breakAnchorC = new ComboBox<>();
        breakAnchorC.getItems().addAll("Початок перерви", "Кінець перерви", "Середина перерви", "Зі зміщенням (хв)");
        breakAnchorC.setValue(event != null ? switch (event.breakAnchor() != null ? event.breakAnchor() : "START") {
            case "START" -> "Початок перерви";
            case "END" -> "Кінець перерви";
            case "MIDDLE" -> "Середина перерви";
            case "OFFSET" -> "Зі зміщенням (хв)";
            default -> "Початок перерви";
        } : "Початок перерви");
        breakAnchorC.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-border-radius: 12; -fx-background-color: white; -fx-border-color: #e2e8f0; -fx-padding: 8;");
        breakAnchorC.setPrefWidth(200);

        offsetF = createStyledField(String.valueOf(event != null ? event.breakOffset() : 0));
        offsetF.setPrefWidth(80);

        VBox dynamicFields = new VBox(20);
        dynamicFields.setAlignment(Pos.CENTER_LEFT);

        Runnable updateFields = () -> {
            dynamicFields.getChildren().clear();
            GridPane dynamicGrid = new GridPane();
            dynamicGrid.setHgap(20);
            dynamicGrid.setVgap(20);
            dynamicGrid.setAlignment(Pos.CENTER_LEFT);
            
            javafx.scene.layout.ColumnConstraints dLabelCol = new javafx.scene.layout.ColumnConstraints();
            dLabelCol.setPrefWidth(130);
            dynamicGrid.getColumnConstraints().add(dLabelCol);

            if (typeC.getValue().equals("На перервах")) {
                dynamicGrid.add(createLabel("КОЛИ ГРАТИ"), 0, 0);
                HBox h = new HBox(15, breakAnchorC);
                h.setAlignment(Pos.CENTER_LEFT);
                if (breakAnchorC.getValue().equals("Зі зміщенням (хв)")) {
                    h.getChildren().add(offsetF);
                }
                dynamicGrid.add(h, 1, 0);
            } else if (typeC.getValue().equals("У конкретний час")) {
                dynamicGrid.add(createLabel("ЧАС"), 0, 0);
                dynamicGrid.add(timeF, 1, 0);
            } else if (typeC.getValue().equals("Разово")) {
                dynamicGrid.add(createLabel("ДАТА"), 0, 0);
                dynamicGrid.add(dateP, 1, 0);
                dynamicGrid.add(createLabel("ЧАС"), 0, 1);
                dynamicGrid.add(timeF, 1, 1);
            }
            dynamicFields.getChildren().add(dynamicGrid);
            if (isShowing()) {
                sizeToScene();
            }
        };

        typeC.valueProperty().addListener((obs, oldV, newV) -> updateFields.run());
        breakAnchorC.valueProperty().addListener((obs, oldV, newV) -> updateFields.run());
        updateFields.run();

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("СКАСУВАТИ");
        cancelBtn.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #636e72; -fx-font-weight: 800; -fx-padding: 12 24; -fx-background-radius: 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ПОДІЮ", ICON_SAVE);
        saveBtn.setOnAction(ev -> {
            if (saveEvent()) {
                close();
            }
        });

        actions.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(headerBox, grid, dynamicFields, actions);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(
            "data:text/css," + MODERN_DATE_PICKER_STYLE.replace(" ", "%20")
        );
        setScene(scene);
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #94a3b8; -fx-letter-spacing: 1px;");
        return lbl;
    }

    private boolean saveEvent() {
        String name = nameF.getText().trim();
        if (name.isEmpty()) {
            ToastService.showError("Назва не може бути порожньою");
            return false;
        }

        String type = switch (typeC.getValue()) {
            case "На перервах" -> "BREAKS";
            case "У конкретний час" -> "TIME";
            case "Разово" -> "ONCE";
            default -> "BREAKS";
        };
        String anchor = switch (breakAnchorC.getValue()) {
            case "Початок перерви" -> "START";
            case "Кінець перерви" -> "END";
            case "Середина перерви" -> "MIDDLE";
            case "Зі зміщенням (хв)" -> "OFFSET";
            default -> "START";
        };
        int offset = 0;
        try {
            offset = Integer.parseInt(offsetF.getText().trim());
        } catch (Exception ignored) {
        }

        String path = pathF.getText();
        if (path == null || path.isEmpty()) {
            ToastService.showError("Оберіть файл або папку");
            return false;
        }

        boolean isFolder = path.equals(event != null ? event.path() : "")
                ? event != null && event.isFolder()
                : new File(path).isDirectory();

        MediaEvent newEvent = new MediaEvent(
                event != null ? event.id() : null,
                name,
                path,
                type,
                timeF.getText(),
                "1,2,3,4,5",
                dateP.getValue().toString(),
                true,
                isFolder,
                0,
                anchor,
                offset
        );

        if (event == null) {
            mainApp.getMediaSchedulerService().addEvent(newEvent);
        } else {
            mainApp.getMediaSchedulerService().updateEvent(newEvent);
        }

        mainApp.showNotifications();
        return true;
    }
}
