package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.MediaEvent;
import com.schoolbell.service.MediaSchedulerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createStyledField;
import static com.schoolbell.ui.ControlFactory.createToggleSwitch;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.BTN_BASE;
import static com.schoolbell.ui.UIStyles.COLOR_BG;
import static com.schoolbell.ui.UIStyles.COLOR_PRIMARY;
import static com.schoolbell.ui.UIStyles.COLOR_SUCCESS;
import static com.schoolbell.ui.UIStyles.HEADER_STYLE;
import static com.schoolbell.ui.UIStyles.ICON_EDIT;
import static com.schoolbell.ui.UIStyles.ICON_FOLDER;
import static com.schoolbell.ui.UIStyles.ICON_MUSIC;
import static com.schoolbell.ui.UIStyles.ICON_PLUS;
import static com.schoolbell.ui.UIStyles.ICON_TRASH;
import static com.schoolbell.ui.UIStyles.MODERN_DATE_PICKER_STYLE;

public class MediaSchedulerPanel {
    private static final String SECTION_CARD =
            "-fx-background-color: rgba(255,255,255,0.96);" +
            "-fx-background-radius: 32;" +
            "-fx-border-color: rgba(226,232,240,0.72);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 32;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.08), 30, 0, 0, 10);";
    private static final String ITEM_CARD =
            "-fx-background-color: linear-gradient(to bottom right, #ffffff, #fbfdff);" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: rgba(226,232,240,0.65);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 22;";

    private final MainApp mainApp;

    public MediaSchedulerPanel(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node build() {
        VBox card = new VBox(20);
        card.setPadding(new Insets(28));
        card.setStyle(SECTION_CARD);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(4);
        Label title = new Label("Автоматичні аудіо-повідомлення");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Керуйте розкладом автоматичного відтворення аудіо для шкільних повідомлень та подій.");
        subtitle.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #64748b;");
        copy.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = createPrimaryActionButton("Додати повідомлення", ICON_PLUS);
        addBtn.setOnAction(e -> showEventDialog(null));

        header.getChildren().addAll(copy, spacer, addBtn);

        VBox list = new VBox(14);
        refreshMediaEventsList(list);

        card.getChildren().addAll(header, list);
        return card;
    }

    private void refreshMediaEventsList(VBox list) {
        list.getChildren().clear();
        MediaSchedulerService service = mainApp.getMediaSchedulerService();
        for (MediaEvent event : service.getEvents()) {
            list.getChildren().add(createMediaEventCard(event, list));
        }
    }

    private HBox createMediaEventCard(MediaEvent event, VBox list) {
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18, 20, 18, 20));
        row.setStyle(ITEM_CARD);

        VBox iconBox = new VBox(createSVGIcon(event.isFolder() ? ICON_FOLDER : ICON_MUSIC, Color.web("#4f46e5"), 20));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(52, 52);
        iconBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #eef2ff, #eff6ff); -fx-background-radius: 18;");

        Label name = new Label(event.name());
        name.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label detail = new Label(describeEvent(event));
        detail.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #64748b;");
        VBox info = new VBox(4, name, detail);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label status = new Label(event.isActive() ? "Активне" : "Вимкнене");
        status.setStyle(
                "-fx-font-family: 'Inter';" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 700;" +
                "-fx-text-fill: " + (event.isActive() ? "#15803d" : "#64748b") + ";" +
                "-fx-background-color: " + (event.isActive() ? "#ecfdf3" : "#f1f5f9") + ";" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 6 12;"
        );

        var toggle = createToggleSwitch(event.isActive());
        toggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            MediaEvent updated = new MediaEvent(event.id(), event.name(), event.path(), event.type(), event.time(), event.daysOfWeek(), event.date(), newVal, event.isFolder(), event.durationMinutes(), event.breakAnchor(), event.breakOffset());
            mainApp.getMediaSchedulerService().updateEvent(updated);
            row.setOpacity(newVal ? 1.0 : 0.72);
            status.setText(newVal ? "Активне" : "Вимкнене");
            status.setStyle(
                    "-fx-font-family: 'Inter';" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: 700;" +
                    "-fx-text-fill: " + (newVal ? "#15803d" : "#64748b") + ";" +
                    "-fx-background-color: " + (newVal ? "#ecfdf3" : "#f1f5f9") + ";" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 6 12;"
            );
        });
        row.setOpacity(event.isActive() ? 1.0 : 0.72);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button edit = createCardActionButton(ICON_EDIT, "#EEF2FF", "#C7D2FE");
        edit.setOnAction(e -> showEventDialog(event));
        Button delete = createCardActionButton(ICON_TRASH, "#FEF2F2", "#FECACA");
        delete.setOnAction(e -> {
            mainApp.getMediaSchedulerService().deleteEvent(event.id());
            refreshMediaEventsList(list);
        });

        actions.getChildren().addAll(status, toggle, edit, delete);
        row.getChildren().addAll(iconBox, info, actions);
        return row;
    }

    private String describeEvent(MediaEvent event) {
        return switch (event.type()) {
            case "BREAKS" -> {
                String anchorText = switch (event.breakAnchor() != null ? event.breakAnchor() : "START") {
                    case "START" -> "початок перерви";
                    case "END" -> "кінець перерви";
                    case "MIDDLE" -> "середина перерви";
                    case "OFFSET" -> "зміщення на " + event.breakOffset() + " хв.";
                    default -> "початок перерви";
                };
                yield "Відтворення: " + anchorText;
            }
            case "TIME" -> "Щодня о " + event.time();
            case "ONCE" -> {
                try {
                    String[] parts = event.date().split("-");
                    yield "Разово: " + parts[2] + "." + parts[1] + "." + parts[0] + " о " + event.time();
                } catch (Exception e) {
                    yield "Разово: " + event.date() + " о " + event.time();
                }
            }
            default -> "";
        };
    }

    private void showEventDialog(MediaEvent event) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(event == null ? "Додати подію" : "Редагування події");

        VBox root = new VBox(24);
        root.setPadding(new Insets(30));
        root.setMinWidth(600);
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Label title = new Label(event == null ? "Додати подію" : "Редагувати подію");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label subtitle = new Label(event == null ? "Налаштування нового автоматичного сповіщення." : event.name());
        subtitle.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #64748b;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER_LEFT);
        
        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        labelCol.setPrefWidth(120);
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        TextField nameF = createStyledField(event != null ? event.name() : "");
        nameF.setPromptText("Назва повідомлення");
        Label nameL = new Label("НАЗВА");
        nameL.setStyle(HEADER_STYLE);
        grid.add(nameL, 0, 0);
        grid.add(nameF, 1, 0);

        ComboBox<String> typeC = new ComboBox<>();
        typeC.getItems().addAll("На перервах", "У конкретний час", "Разово");
        typeC.setValue(event != null ? switch (event.type()) {
            case "BREAKS" -> "На перервах";
            case "TIME" -> "У конкретний час";
            case "ONCE" -> "Разово";
            default -> "На перервах";
        } : "На перервах");
        typeC.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #dfe6e9; -fx-border-radius: 12;");
        typeC.setMaxWidth(Double.MAX_VALUE);
        Label typeL = new Label("ТИП ТРИГЕРА");
        typeL.setStyle(HEADER_STYLE);
        grid.add(typeL, 0, 1);
        grid.add(typeC, 1, 1);

        TextField pathF = createStyledField(event != null ? event.path() : "");
        pathF.setEditable(false);
        pathF.setMaxWidth(Double.MAX_VALUE);

        Button browseFile = createPrimaryActionButton("Файл", ICON_MUSIC);
        Button browseFolder = createPrimaryActionButton("Папка", ICON_FOLDER);
        browseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо файли (MP3, WAV)", "*.mp3", "*.wav"));
            File f = fc.showOpenDialog(stage);
            if (f != null) pathF.setText(f.getAbsolutePath());
        });
        browseFolder.setOnAction(e -> {
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            File f = dc.showDialog(stage);
            if (f != null) pathF.setText(f.getAbsolutePath());
        });

        VBox pathWrapper = new VBox(10, pathF, new HBox(10, browseFile, browseFolder));
        Label pathL = new Label("ДЖЕРЕЛО ЗВУКУ");
        pathL.setStyle(HEADER_STYLE);
        grid.add(pathL, 0, 2);
        grid.add(pathWrapper, 1, 2);

        TextField timeF = createStyledField(event != null ? event.time() : "12:00");
        timeF.setPrefWidth(120);

        DatePicker dateP = new DatePicker(event != null && event.date() != null && !event.date().isEmpty()
                ? java.time.LocalDate.parse(event.date())
                : java.time.LocalDate.now());
        dateP.setStyle(MODERN_DATE_PICKER_STYLE);
        dateP.setPrefWidth(200);

        ComboBox<String> breakAnchorC = new ComboBox<>();
        breakAnchorC.getItems().addAll("Початок перерви", "Кінець перерви", "Середина перерви", "Зі зміщенням (хв)");
        breakAnchorC.setValue(event != null ? switch (event.breakAnchor() != null ? event.breakAnchor() : "START") {
            case "START" -> "Початок перерви";
            case "END" -> "Кінець перерви";
            case "MIDDLE" -> "Середина перерви";
            case "OFFSET" -> "Зі зміщенням (хв)";
            default -> "Початок перерви";
        } : "Початок перерви");
        breakAnchorC.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #dfe6e9; -fx-border-radius: 12;");
        breakAnchorC.setPrefWidth(200);

        TextField offsetF = createStyledField(String.valueOf(event != null ? event.breakOffset() : 0));
        offsetF.setPrefWidth(80);

        VBox dynamicFields = new VBox(20);
        dynamicFields.setAlignment(Pos.CENTER_LEFT);

        Runnable updateFields = () -> {
            dynamicFields.getChildren().clear();
            GridPane dynamicGrid = new GridPane();
            dynamicGrid.setHgap(20);
            dynamicGrid.setVgap(20);
            dynamicGrid.setAlignment(Pos.CENTER_LEFT);

            if (typeC.getValue().equals("На перервах")) {
                Label anchorL = new Label("КОЛИ ГРАТИ");
                anchorL.setStyle(HEADER_STYLE);
                anchorL.setPrefWidth(120); // Задаємо фіксовану ширину для вирівнювання
                dynamicGrid.add(anchorL, 0, 0);
                HBox h = new HBox(15, breakAnchorC);
                h.setAlignment(Pos.CENTER_LEFT);
                if (breakAnchorC.getValue().equals("Зі зміщенням (хв)")) {
                    h.getChildren().add(offsetF);
                }
                dynamicGrid.add(h, 1, 0);
            } else if (typeC.getValue().equals("У конкретний час")) {
                Label timeL = new Label("ЧАС");
                timeL.setStyle(HEADER_STYLE);
                timeL.setPrefWidth(120);
                dynamicGrid.add(timeL, 0, 0);
                dynamicGrid.add(timeF, 1, 0);
            } else if (typeC.getValue().equals("Разово")) {
                Label dateL = new Label("ДАТА");
                dateL.setStyle(HEADER_STYLE);
                dateL.setPrefWidth(120);
                dynamicGrid.add(dateL, 0, 0);
                dynamicGrid.add(dateP, 1, 0);
                
                Label timeL = new Label("ЧАС");
                timeL.setStyle(HEADER_STYLE);
                timeL.setPrefWidth(120);
                dynamicGrid.add(timeL, 0, 1);
                dynamicGrid.add(timeF, 1, 1);
            }
            dynamicFields.getChildren().add(dynamicGrid);
            if (stage.isShowing()) {
                stage.sizeToScene();
            }
        };

        typeC.valueProperty().addListener((obs, oldV, newV) -> updateFields.run());
        breakAnchorC.valueProperty().addListener((obs, oldV, newV) -> updateFields.run());
        updateFields.run();

        Button saveBtn = new Button("ЗБЕРЕГТИ ПОДІЮ");
        saveBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 12 60;");
        saveBtn.setOnAction(ev -> {
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

            boolean isFolder = pathF.getText().equals(event != null ? event.path() : "")
                    ? event != null && event.isFolder()
                    : new File(pathF.getText()).isDirectory();
            MediaEvent newEvent = new MediaEvent(
                    event != null ? event.id() : null,
                    nameF.getText(),
                    pathF.getText(),
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
            stage.close();
        });

        root.getChildren().addAll(title, subtitle, grid, dynamicFields, new HBox(saveBtn));
        ((HBox) root.getChildren().get(root.getChildren().size() - 1)).setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        scene.getStylesheets().add("data:text/css," + MODERN_DATE_PICKER_STYLE.replace(" ", "%20"));
        stage.setScene(scene);
        stage.showAndWait();
    }
}
