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

public class MediaEventEditorDialog extends BasePremiumDialog {
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
        super(mainApp.getStage(),
                event == null ? "Додати подію" : "Редагувати подію",
                "Налаштування події",
                event == null ? "Налаштування нового автоматичного сповіщення." : event.name(),
                "ЗБЕРЕГТИ ПОДІЮ");

        this.mainApp = mainApp;
        this.event = event;

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
        typeC.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-border-radius: 12; -fx-background-color: white; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-padding: 8;");
        typeC.setMaxWidth(Double.MAX_VALUE);
        grid.add(createLabel("ТИП ТРИГЕРА"), 0, 1);
        grid.add(typeC, 1, 1);

        // --- MODERN SOURCE WIDGET ---
        VBox sourceCard = new VBox(15);
        sourceCard.setPadding(new Insets(20));
        sourceCard.setStyle("-fx-background-color: " + COLOR_SURFACE_SKY + "; -fx-background-radius: 18; -fx-border-color: " + COLOR_BORDER_FIELD + "; -fx-border-width: 1.5; -fx-border-dash-array: 6 6;");

        HBox sourceInfo = new HBox(15);
        sourceInfo.setAlignment(Pos.CENTER_LEFT);

        VBox pathIconBox = new VBox();
        pathIconBox.setAlignment(Pos.CENTER);
        pathIconBox.setPrefSize(50, 50);
        pathIconBox.setStyle(ICON_BADGE_STYLE + "-fx-background-radius: 14;");

        VBox pathTextStack = new VBox(2);
        Label pathMainLabel = new Label("Джерело не обрано");
        pathMainLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: " + COLOR_NAVY + ";");
        pathMainLabel.setWrapText(true);
        pathMainLabel.setMaxWidth(420);

        Label pathSubLabel = new Label("Оберіть аудіофайл або папку для програвання");
        pathSubLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLOR_SLATE + ";");
        pathSubLabel.setEllipsisString("...");
        pathSubLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        pathSubLabel.setMaxWidth(420);

        pathTextStack.getChildren().addAll(pathMainLabel, pathSubLabel);

        sourceInfo.getChildren().addAll(pathIconBox, pathTextStack);

        pathF = new TextField(event != null ? event.path() : ""); // Hidden but used for logic
        pathF.setManaged(false);
        pathF.setVisible(false);

        Runnable updateSourceDisplay = () -> {
            String currentPath = pathF.getText();
            if (currentPath == null || currentPath.isEmpty()) {
                pathIconBox.getChildren().setAll(createSVGIcon(ICON_MUSIC, Color.web(COLOR_SLATE_LIGHT), 24));
                pathMainLabel.setText("Джерело не обрано");
                pathSubLabel.setText("Оберіть аудіофайл або папку");
                sourceCard.setStyle(sourceCard.getStyle() + "-fx-border-color: " + COLOR_BORDER_FIELD + ";");
            } else {
                File file = new File(currentPath);
                boolean exists = file.exists();
                boolean isDir = file.isDirectory();

                pathIconBox.getChildren().setAll(createSVGIcon(isDir ? ICON_FOLDER : ICON_MUSIC, Color.web(exists ? COLOR_PRIMARY : COLOR_DANGER), 24));
                pathMainLabel.setText(file.getName().isEmpty() ? currentPath : file.getName());
                
                if (!exists) {
                    pathMainLabel.setStyle(pathMainLabel.getStyle() + "-fx-text-fill: " + COLOR_DANGER + ";");
                    pathSubLabel.setText("УВАГА: Файл або папка не існує!");
                    pathSubLabel.setStyle(pathSubLabel.getStyle() + "-fx-text-fill: " + COLOR_DANGER + "; -fx-font-weight: 800;");
                    sourceCard.setStyle(sourceCard.getStyle().replace(COLOR_BORDER_FIELD, COLOR_DANGER).replace(COLOR_INDIGO, COLOR_DANGER) + "-fx-border-style: solid; -fx-background-color: " + COLOR_DANGER_PALE + ";");
                } else {
                    pathMainLabel.setStyle(pathMainLabel.getStyle().replace("-fx-text-fill: " + COLOR_DANGER + ";", "") + "-fx-text-fill: " + COLOR_NAVY + ";");
                    pathSubLabel.setText(currentPath);
                    pathSubLabel.setStyle(pathSubLabel.getStyle().replace("-fx-text-fill: " + COLOR_DANGER + "; -fx-font-weight: 800;", "") + "-fx-text-fill: " + COLOR_SLATE + ";");
                    sourceCard.setStyle(sourceCard.getStyle().replace(COLOR_DANGER, COLOR_INDIGO) + "-fx-border-style: solid; -fx-background-color: " + COLOR_SURFACE_SKY + ";");
                }
                pathSubLabel.setTooltip(new Tooltip(currentPath));
            }
        };

        Button browseFile = new Button("ОБРАТИ ФАЙЛ");
        browseFile.setGraphic(createSVGIcon(ICON_MUSIC, Color.web(COLOR_PRIMARY), 14));
        browseFile.setStyle("-fx-background-color: white; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 8 16; -fx-background-radius: 10; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 10; -fx-cursor: hand;");

        Button browseFolder = new Button("ОБРАТИ ПАПКУ");
        browseFolder.setGraphic(createSVGIcon(ICON_FOLDER, Color.web(COLOR_VIOLET), 14));
        browseFolder.setStyle("-fx-background-color: white; -fx-text-fill: " + COLOR_VIOLET + "; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 8 16; -fx-background-radius: 10; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 10; -fx-cursor: hand;");

        browseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо файли (MP3, WAV)", "*.mp3", "*.wav"));
            File f = fc.showOpenDialog(this);
            if (f != null) {
                pathF.setText(f.getAbsolutePath());
                updateSourceDisplay.run();
            }
        });
        browseFolder.setOnAction(e -> {
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            File f = dc.showDialog(this);
            if (f != null) {
                pathF.setText(f.getAbsolutePath());
                updateSourceDisplay.run();
            }
        });

        HBox btnRow = new HBox(12, browseFile, browseFolder);
        sourceCard.getChildren().addAll(sourceInfo, btnRow);

        grid.add(createLabel("ДЖЕРЕЛО ЗВУКУ"), 0, 2);
        grid.add(sourceCard, 1, 2);

        updateSourceDisplay.run();

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
        breakAnchorC.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-border-radius: 12; -fx-background-color: white; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-padding: 8;");
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
            dLabelCol.setPrefWidth(170);
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

        content.getChildren().addAll(grid, dynamicFields);
    }

    @Override
    protected boolean onSave() {
        return saveEvent();
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE_LIGHT + "; -fx-letter-spacing: 1px;");
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

        return true;
    }
}
