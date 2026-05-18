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
        Label subtitle = new Label("Компактний список подій з пріоритетом на назві, статусі та швидких діях.");
        subtitle.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #64748b;");
        copy.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = createPrimaryActionButton("Додати повідомлення", ICON_PLUS);
        addBtn.setStyle(
                "-fx-font-family: 'Inter';" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 700;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: linear-gradient(to right, #4f46e5, #2563eb);" +
                "-fx-background-radius: 18;" +
                "-fx-padding: 14 22;" +
                "-fx-cursor: hand;"
        );
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

        MenuButton more = new MenuButton();
        more.setGraphic(createSVGIcon("M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z", Color.web("#475569"), 18));
        more.setStyle(
                "-fx-background-color: rgba(248,250,252,0.96);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(226,232,240,0.85);" +
                "-fx-border-radius: 18;" +
                "-fx-padding: 4;"
        );
        MenuItem editItem = new MenuItem("Редагувати");
        editItem.setOnAction(e -> showEventDialog(event));
        MenuItem deleteItem = new MenuItem("Видалити");
        deleteItem.setOnAction(e -> {
            mainApp.getMediaSchedulerService().deleteEvent(event.id());
            refreshMediaEventsList(list);
        });
        more.getItems().addAll(editItem, deleteItem);

        actions.getChildren().addAll(status, toggle, edit, delete, more);
        row.getChildren().addAll(iconBox, info, actions);
        return row;
    }

    private String describeEvent(MediaEvent event) {
        return switch (event.type()) {
            case "BREAKS" -> "Автоматично: " + (event.breakAnchor() != null ? event.breakAnchor() : "Початок перерви");
            case "TIME" -> "Щодня о " + event.time();
            case "ONCE" -> "Разово: " + event.date() + " о " + event.time();
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

        TextField nameF = createStyledField(event != null ? event.name() : "");
        nameF.setPrefWidth(350);
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
        pathF.setPrefWidth(250);
        HBox.setHgrow(pathF, Priority.ALWAYS);

        Button browseFile = createPrimaryActionButton("Файл", ICON_MUSIC);
        Button browseFolder = createPrimaryActionButton("Папка", ICON_FOLDER);
        browseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Оберіть аудіофайл");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо файли (MP3, WAV)", "*.mp3", "*.wav"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                pathF.setText(f.getAbsolutePath());
            }
        });
        browseFolder.setOnAction(e -> {
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            dc.setTitle("Оберіть папку з аудіо");
            File f = dc.showDialog(stage);
            if (f != null) {
                pathF.setText(f.getAbsolutePath());
            }
        });

        HBox pathBox = new HBox(12, pathF, browseFile, browseFolder);
        Label pathL = new Label("ДЖЕРЕЛО ЗВУКУ");
        pathL.setStyle(HEADER_STYLE);
        grid.add(pathL, 0, 2);
        grid.add(pathBox, 1, 2);

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
            if (typeC.getValue().equals("На перервах")) {
                Label anchorL = new Label("КОЛИ ГРАТИ");
                anchorL.setStyle(HEADER_STYLE);
                HBox h = new HBox(15, anchorL, breakAnchorC);
                h.setAlignment(Pos.CENTER_LEFT);
                if (breakAnchorC.getValue().equals("Зі зміщенням (хв)")) {
                    h.getChildren().add(offsetF);
                }
                dynamicFields.getChildren().add(h);
            } else if (typeC.getValue().equals("У конкретний час")) {
                Label timeL = new Label("ЧАС ВІДТВОРЕННЯ");
                timeL.setStyle(HEADER_STYLE);
                dynamicFields.getChildren().add(new HBox(20, timeL, timeF));
            } else if (typeC.getValue().equals("Разово")) {
                Label dateL = new Label("ДАТА");
                dateL.setStyle(HEADER_STYLE);
                Label timeL = new Label("ЧАС");
                timeL.setStyle(HEADER_STYLE);
                dynamicFields.getChildren().addAll(new HBox(20, dateL, dateP), new HBox(20, timeL, timeF));
            }
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
