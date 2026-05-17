package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.MediaEvent;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.MediaSchedulerService;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.File;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class NotificationsView {
    private final MainApp mainApp;
    private final ConfigService config;

    private ComboBox<String> deviceCombo;
    private int currentVolumeValue;
    private HBox volumePresetBox;

    // Air Raid
    private ToggleButton arAudioTg;
    private TextField arAudioPath;
    private ToggleButton arVisualTg;

    // Emergency
    private ToggleButton emAudioTg;
    private TextField emAudioPath;
    private ToggleButton emVisualTg;

    // Silence
    private ToggleButton siAudioTg;
    private TextField siAudioPath;
    private ToggleButton siVisualTg;

    public NotificationsView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
    }

    public Node build() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ВСЕ", ICON_SAVE);
        saveBtn.setOnAction(e -> save());

        VBox headerArea = createSectionHeader(
                "Сигнали та сповіщення",
                "Централізоване керування екстреними сигналами та мультимедіа",
                "#2d3436",
                ICON_NOTIFICATIONS,
                saveBtn
        );

        HBox content = new HBox(25);
        VBox mainCol = new VBox(25);
        HBox.setHgrow(mainCol, Priority.ALWAYS);

        // --- GLOBAL AUDIO SETTINGS ---
        mainCol.getChildren().add(createAudioOutputCard());

        // --- CONSOLIDATED ALERTS PANEL ---
        VBox alertsPanel = new VBox(0);
        alertsPanel.setStyle(SOFT_CARD + "-fx-padding: 0;");

        Label panelTitle = new Label("КОНФІГУРАЦІЯ ЕКСТРЕНИХ СИГНАЛІВ");
        panelTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-padding: 20 25; -fx-letter-spacing: 1px;");
        
        VBox alertsList = new VBox(0);
        
        alertsList.getChildren().add(createAlertRow("ПОВІТРЯНА ТРИВОГА", ICON_AIR_RAID, "#f39c12",
                arAudioTg = createToggleSwitch(config.isAudioAirRaidEnabled()), 
                arAudioPath = new TextField(config.getAudioAirRaidPath()), 
                arVisualTg = createToggleSwitch(config.isVisualAirRaidEnabled()),
                "AIR_RAID", true));

        alertsList.getChildren().add(createAlertRow("ЕКСТРЕНА СИТУАЦІЯ", ICON_LIFEBUOY, COLOR_DANGER,
                emAudioTg = createToggleSwitch(config.isAudioEmergencyEnabled()), 
                emAudioPath = new TextField(config.getAudioEmergencyPath()), 
                emVisualTg = createToggleSwitch(config.isVisualEmergencyEnabled()),
                "EMERGENCY", true));

        alertsList.getChildren().add(createAlertRow("ХВИЛИНА МОВЧАННЯ", ICON_CLOCK, COLOR_PRIMARY,
                siAudioTg = createToggleSwitch(config.isAudioSilenceEnabled()), 
                siAudioPath = new TextField(config.getAudioSilencePath()), 
                siVisualTg = createToggleSwitch(config.isVisualSilenceEnabled()),
                "SILENCE", false));

        alertsPanel.getChildren().addAll(panelTitle, alertsList);
        mainCol.getChildren().add(alertsPanel);

        // --- MEDIA EVENTS PANEL ---
        mainCol.getChildren().add(buildMediaEventsCard());

        VBox helpPanel = createSideHelpPanel(
            createHelpCard(ICON_VOLUME, "Аудіо", "Налаштуйте гучність та пристрій для трансляції сигналів.", COLOR_SUCCESS),
            createHelpCard(ICON_MONITOR, "Табло", "Сигнали з'являться на всіх підключених екранах автоматично.", COLOR_PURPLE),
            createHelpCard(ICON_INFO, "Тест", "Використовуйте великі кнопки праворуч для перевірки.", "#fdcb6e")
        );

        content.getChildren().addAll(mainCol, helpPanel);
        root.getChildren().addAll(headerArea, content);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private VBox createAudioOutputCard() {
        VBox card = new VBox(25);
        card.setPadding(new Insets(30));
        card.setStyle(SOFT_CARD + "-fx-padding: 30;");

        HBox layout = new HBox(40);
        layout.setAlignment(Pos.CENTER_LEFT);

        // --- Left: Device Selection ---
        VBox devBox = new VBox(12);
        HBox.setHgrow(devBox, Priority.ALWAYS);
        
        Label devLabel = new Label("ПРИСТРІЙ ВІДТВОРЕННЯ");
        devLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 1px;");
        
        deviceCombo = new ComboBox<>();
        deviceCombo.setMaxWidth(Double.MAX_VALUE);
        deviceCombo.setStyle(COMBO_STYLE + "-fx-font-weight: 700; -fx-padding: 8 12;");
        deviceCombo.setValue(config.getSelectedAudioDeviceName());
        try {
            deviceCombo.getItems().add("Системний за замовчуванням");
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                if (AudioSystem.getMixer(info).isLineSupported(new DataLine.Info(SourceDataLine.class, new AudioFormat(44100, 16, 2, true, false)))) {
                    deviceCombo.getItems().add(info.getName());
                }
            }
        } catch (Exception ignored) {}

        HBox devInput = new HBox(15, 
            new VBox(createSVGIcon(ICON_VOLUME, Color.web(COLOR_PRIMARY), 22)),
            deviceCombo
        );
        devInput.setAlignment(Pos.CENTER_LEFT);
        ((VBox)devInput.getChildren().get(0)).setPadding(new Insets(0, 0, 0, 5));
        
        devBox.getChildren().addAll(devLabel, devInput);

        // --- Right: Volume Presets ---
        VBox volBox = new VBox(12);
        volBox.setMinWidth(350);
        
        Label volLabel = new Label("ЗАГАЛЬНА ГУЧНІСТЬ СПОВІЩЕНЬ");
        volLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 1px;");

        currentVolumeValue = config.getSystemVolume();
        volumePresetBox = new HBox(8);
        volumePresetBox.setAlignment(Pos.CENTER_LEFT);
        
        int[] presets = {0, 25, 50, 75, 100};
        for (int p : presets) {
            Button pb = new Button(p == 0 ? "ВИМК" : p + "%");
            pb.setPrefWidth(60);
            pb.setUserData(p);
            pb.setOnAction(e -> {
                currentVolumeValue = p;
                updateVolumeStyle();
                mainApp.getAudioService().setVolume(p);
                mainApp.getSystemService().setWindowsSystemVolume(p);
            });
            volumePresetBox.getChildren().add(pb);
        }
        
        updateVolumeStyle();
        volBox.getChildren().addAll(volLabel, volumePresetBox);

        layout.getChildren().addAll(devBox, volBox);
        card.getChildren().add(layout);
        return card;
    }

    private void updateVolumeStyle() {
        for (Node n : volumePresetBox.getChildren()) {
            if (n instanceof Button b) {
                int val = (int) b.getUserData();
                if (val == currentVolumeValue) {
                    b.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white; -fx-font-weight: 900; -fx-background-radius: 10; -fx-padding: 8 0;");
                } else {
                    b.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #636e72; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 0; -fx-cursor: hand;");
                }
            }
        }
    }

    private VBox createAlertRow(String title, String icon, String color, ToggleButton audioTg, TextField pathField, ToggleButton visualTg, String alertType, boolean showSeparator) {
        VBox root = new VBox();
        HBox row = new HBox(30);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(25, 30, 25, 30));
        row.setStyle("-fx-background-color: white;");

        // 1. IDENTITY
        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(color), 28));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(56, 56);
        iconBox.setMinSize(56, 56);
        iconBox.setStyle("-fx-background-color: " + color + "12; -fx-background-radius: 18;");
        
        VBox titleBox = new VBox(2);
        Label tType = new Label("ТИП СИГНАЛУ");
        tType.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 0.5px;");
        Label tMain = new Label(title);
        tMain.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        titleBox.getChildren().addAll(tType, tMain);
        titleBox.setMinWidth(200);

        // 2. CONFIGURATION (AUDIO & VISUAL)
        VBox configArea = new VBox(15);
        HBox.setHgrow(configArea, Priority.ALWAYS);

        // Audio Sub-row
        HBox audioRow = new HBox(20);
        audioRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox audioToggleBox = new VBox(4, audioTg, new Label("АУДІО"));
        audioToggleBox.setAlignment(Pos.CENTER);
        ((Label)audioToggleBox.getChildren().get(1)).setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        
        pathField.setEditable(false);
        pathField.setPromptText("Шлях до аудіофайлу...");
        pathField.setStyle(FIELD_STYLE + "-fx-background-color: #f8f9fa; -fx-font-size: 13px; -fx-border-color: #f1f2f6;");
        HBox.setHgrow(pathField, Priority.ALWAYS);
        
        Button browse = createCardActionButton(ICON_FOLDER, "#f1f2f6", COLOR_PRIMARY);
        browse.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Оберіть аудіофайл");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо файли (MP3, WAV)", "*.mp3", "*.wav"));
            File f = fc.showOpenDialog(mainApp.getStage());
            if (f != null) pathField.setText(f.getAbsolutePath());
        });
        
        audioRow.getChildren().addAll(audioToggleBox, pathField, browse);

        // Visual Sub-row (Inline with Audio or below) - Let's put it next to test buttons for compactness
        configArea.getChildren().add(audioRow);

        // 3. VISUAL TOGGLE & TEST ACTIONS
        HBox actionsRow = new HBox(25);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        VBox visualToggleBox = new VBox(4, visualTg, new Label("ЕКРАН"));
        visualToggleBox.setAlignment(Pos.CENTER);
        ((Label)visualToggleBox.getChildren().get(1)).setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + ";");

        HBox testButtons = new HBox(12);
        testButtons.setAlignment(Pos.CENTER_LEFT);
        
        Button tAudio = createCardActionButton(ICON_VOLUME, COLOR_GREEN_LIGHT, COLOR_SUCCESS);
        tAudio.setOnAction(e -> {
            if (!pathField.getText().isEmpty()) mainApp.getAudioService().playAudioFile(pathField.getText());
            else java.awt.Toolkit.getDefaultToolkit().beep();
        });
        
        Button tVisual = createCardActionButton(ICON_MONITOR, COLOR_PURPLE_LIGHT, COLOR_PURPLE);
        tVisual.setOnAction(e -> mainApp.getSignalService().setTemporaryAlertType(alertType, 5000));
        
        testButtons.getChildren().addAll(tAudio, tVisual);
        
        actionsRow.getChildren().addAll(visualToggleBox, new Separator(javafx.geometry.Orientation.VERTICAL), testButtons);

        row.getChildren().addAll(iconBox, titleBox, configArea, actionsRow);
        root.getChildren().add(row);
        
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #fcfcfc;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: white;"));

        if (showSeparator) {
            Separator sep = new Separator();
            sep.setPadding(new Insets(0, 30, 0, 30));
            sep.setStyle("-fx-opacity: 0.3;");
            root.getChildren().add(sep);
        }
        return root;
    }

    private void save() {
        config.setSelectedAudioDeviceName(deviceCombo.getValue());
        config.setSystemVolume(currentVolumeValue);

        config.setAudioAirRaidEnabled(arAudioTg.isSelected());
        config.setAudioAirRaidPath(arAudioPath.getText());
        config.setVisualAirRaidEnabled(arVisualTg.isSelected());

        config.setAudioEmergencyEnabled(emAudioTg.isSelected());
        config.setAudioEmergencyPath(emAudioPath.getText());
        config.setVisualEmergencyEnabled(emVisualTg.isSelected());

        config.setAudioSilenceEnabled(siAudioTg.isSelected());
        config.setAudioSilencePath(siAudioPath.getText());
        config.setVisualSilenceEnabled(siVisualTg.isSelected());

        mainApp.saveConfig();
        ToastService.showSuccess("Налаштування сповіщень збережено!");
    }

    private VBox buildMediaEventsCard() {
        VBox card = new VBox(0);
        card.setStyle(SOFT_CARD + "-fx-padding: 0;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 25, 20, 25));
        
        Label title = new Label("АВТОМАТИЧНІ АУДІО-ПОВІДОМЛЕННЯ");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 1px;");
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button addBtn = new Button("ДОДАТИ ПОДІЮ");
        addBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_PRIMARY + "; -fx-padding: 6 15; -fx-font-size: 11px;");
        addBtn.setGraphic(createSVGIcon(ICON_PLUS, Color.WHITE, 14));
        addBtn.setOnAction(e -> showEventDialog(null));
        
        header.getChildren().addAll(title, spacer, addBtn);
        
        VBox list = new VBox(0);
        refreshMediaEventsList(list);
        
        card.getChildren().addAll(header, list);
        return card;
    }

    private void refreshMediaEventsList(VBox list) {
        list.getChildren().clear();
        MediaSchedulerService svc = mainApp.getMediaSchedulerService();
        for (MediaEvent event : svc.getEvents()) {
            list.getChildren().add(createMediaEventRow(event));
            Separator s = new Separator(); s.setStyle("-fx-opacity: 0.2;");
            list.getChildren().add(s);
        }
    }

    private HBox createMediaEventRow(MediaEvent event) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 25, 15, 25));
        row.setStyle("-fx-background-color: white;");

        VBox iconBox = new VBox(createSVGIcon(event.isFolder() ? ICON_FOLDER : ICON_MUSIC, Color.web(COLOR_PRIMARY), 20));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + COLOR_PRIMARY + "12; -fx-background-radius: 12;");

        VBox info = new VBox(2);
        Label name = new Label(event.name());
        name.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: " + COLOR_TEXT + ";");
        
        String desc = switch(event.type()) {
            case "BREAKS" -> {
                String anchor = switch(event.breakAnchor() != null ? event.breakAnchor() : "START") {
                    case "START" -> "Початок перерви";
                    case "END" -> "Кінець перерви";
                    case "MIDDLE" -> "Середина перерви";
                    case "OFFSET" -> "Зі зміщенням " + event.breakOffset() + " хв";
                    default -> "На перервах";
                };
                yield "Автоматично: " + anchor;
            }
            case "TIME" -> "Щодня о " + event.time();
            case "ONCE" -> "Разово: " + event.date() + " " + event.time();
            default -> "";
        };
        Label detail = new Label(desc);
        detail.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #636e72;");
        info.getChildren().addAll(name, detail);
        HBox.setHgrow(info, Priority.ALWAYS);

        ToggleButton tg = createToggleSwitch(event.isActive());
        tg.selectedProperty().addListener((obs, oldVal, newVal) -> {
            MediaEvent updated = new MediaEvent(event.id(), event.name(), event.path(), event.type(), event.time(), event.daysOfWeek(), event.date(), newVal, event.isFolder(), event.durationMinutes(), event.breakAnchor(), event.breakOffset());
            mainApp.getMediaSchedulerService().updateEvent(updated);
        });

        Button test = createCardActionButton(ICON_VOLUME, COLOR_GREEN_LIGHT, COLOR_SUCCESS);
        test.setOnAction(e -> mainApp.getAudioService().playAudioFile(event.path()));

        Button edit = createCardActionButton(ICON_EDIT, COLOR_BLUE_LIGHT, COLOR_PRIMARY);
        edit.setOnAction(e -> showEventDialog(event));

        Button del = createCardActionButton(ICON_TRASH, "#fff5f5", COLOR_DANGER);
        del.setOnAction(e -> {
            mainApp.getMediaSchedulerService().deleteEvent(event.id());
            refreshMediaEventsList((VBox)row.getParent());
        });

        row.getChildren().addAll(iconBox, info, tg, test, edit, del);
        return row;
    }

    private void showEventDialog(MediaEvent event) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(event == null ? "Додати медіа-подію" : "Редагування події");

        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setMinWidth(600);
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        VBox header = createSectionHeader(
                event == null ? "Додати подію" : "Редагувати подію",
                event == null ? "Налаштування нового автоматичного сповіщення" : event.name(),
                COLOR_PRIMARY,
                ICON_MUSIC
        );

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER_LEFT);

        TextField nameF = createStyledField(event != null ? event.name() : "");
        nameF.setPrefWidth(350);
        nameF.setAlignment(Pos.CENTER_LEFT);
        nameF.setPromptText("Назва (напр. Фонова музика)");
        Label nameL = new Label("НАЗВА");
        nameL.setStyle(HEADER_STYLE);
        grid.add(nameL, 0, 0);
        grid.add(nameF, 1, 0);
        
        ComboBox<String> typeC = new ComboBox<>();
        typeC.getItems().addAll("На перервах", "У конкретний час", "Разово");
        typeC.setValue(event != null ? (switch(event.type()) {
            case "BREAKS" -> "На перервах";
            case "TIME" -> "У конкретний час";
            case "ONCE" -> "Разово";
            default -> "На перервах";
        }) : "На перервах");
        typeC.setStyle(COMBO_STYLE);
        typeC.setMaxWidth(Double.MAX_VALUE);
        Label typeL = new Label("ТИП ТРИГЕРА");
        typeL.setStyle(HEADER_STYLE);
        grid.add(typeL, 0, 1);
        grid.add(typeC, 1, 1);

        TextField pathF = createStyledField(event != null ? event.path() : "");
        pathF.setEditable(false);
        pathF.setAlignment(Pos.CENTER_LEFT);
        pathF.setPrefWidth(250);
        HBox.setHgrow(pathF, Priority.ALWAYS);
        
        Button browseFile = createPrimaryActionButton("ФАЙЛ", ICON_MUSIC);
        Button browseFolder = createPrimaryActionButton("ПАПКА", ICON_FOLDER);
        browseFile.setPadding(new Insets(10, 20, 10, 20));
        browseFolder.setPadding(new Insets(10, 20, 10, 20));

        browseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Оберіть аудіофайл");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо файли (MP3, WAV)", "*.mp3", "*.wav"));
            File f = fc.showOpenDialog(stage);
            if (f != null) pathF.setText(f.getAbsolutePath());
        });

        browseFolder.setOnAction(e -> {
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            dc.setTitle("Оберіть папку з аудіо");
            File f = dc.showDialog(stage);
            if (f != null) pathF.setText(f.getAbsolutePath());
        });

        HBox pathBox = new HBox(12, pathF, browseFile, browseFolder);
        pathBox.setAlignment(Pos.CENTER_LEFT);
        Label pathL = new Label("ДЖЕРЕЛО ЗВУКУ");
        pathL.setStyle(HEADER_STYLE);
        grid.add(pathL, 0, 2);
        grid.add(pathBox, 1, 2);

        TextField timeF = createStyledField(event != null ? event.time() : "12:00");
        timeF.setPrefWidth(120);
        
        DatePicker dateP = new DatePicker(event != null && event.date() != null && !event.date().isEmpty() ? java.time.LocalDate.parse(event.date()) : java.time.LocalDate.now());
        dateP.setStyle(MODERN_DATE_PICKER_STYLE);
        dateP.setPrefWidth(200);

        ComboBox<String> breakAnchorC = new ComboBox<>();
        breakAnchorC.getItems().addAll("Початок перерви", "Кінець перерви", "Середина перерви", "Зі зміщенням (хв)");
        breakAnchorC.setValue(event != null ? (switch(event.breakAnchor() != null ? event.breakAnchor() : "START") {
            case "START" -> "Початок перерви";
            case "END" -> "Кінець перерви";
            case "MIDDLE" -> "Середина перерви";
            case "OFFSET" -> "Зі зміщенням (хв)";
            default -> "Початок перерви";
        }) : "Початок перерви");
        breakAnchorC.setStyle(COMBO_STYLE);
        breakAnchorC.setPrefWidth(200);

        TextField offsetF = createStyledField(String.valueOf(event != null ? event.breakOffset() : 0));
        offsetF.setPrefWidth(80);

        VBox dynamicFields = new VBox(20);
        dynamicFields.setAlignment(Pos.CENTER_LEFT);
        
        Runnable updateFields = () -> {
            dynamicFields.getChildren().clear();
            if (typeC.getValue().equals("На перервах")) {
                Label anchorL = new Label("КОЛИ ГРАТИ:");
                anchorL.setStyle(HEADER_STYLE);
                HBox h = new HBox(15, anchorL, breakAnchorC);
                h.setAlignment(Pos.CENTER_LEFT);
                if (breakAnchorC.getValue().equals("Зі зміщенням (хв)")) {
                    h.getChildren().add(offsetF);
                }
                dynamicFields.getChildren().add(h);
            } else if (typeC.getValue().equals("У конкретний час")) {
                Label timeL = new Label("ЧАС ВІДТВОРЕННЯ:");
                timeL.setStyle(HEADER_STYLE);
                HBox h = new HBox(20, timeL, timeF);
                h.setAlignment(Pos.CENTER_LEFT);
                dynamicFields.getChildren().add(h);
            } else if (typeC.getValue().equals("Разово")) {
                VBox v = new VBox(15);
                Label dateL = new Label("ДАТА:");
                dateL.setStyle(HEADER_STYLE);
                HBox d = new HBox(20, dateL, dateP);
                d.setAlignment(Pos.CENTER_LEFT);
                Label timeL = new Label("ЧАС:");
                timeL.setStyle(HEADER_STYLE);
                HBox t = new HBox(20, timeL, timeF);
                t.setAlignment(Pos.CENTER_LEFT);
                v.getChildren().addAll(d, t);
                dynamicFields.getChildren().add(v);
            }
            if (stage.isShowing()) stage.sizeToScene();
        };
        
        typeC.valueProperty().addListener((obs, oldV, newV) -> updateFields.run());
        breakAnchorC.valueProperty().addListener((obs, oldV, newV) -> updateFields.run());
        updateFields.run();

        Button saveBtn = new Button("ЗБЕРЕГТИ ПОДІЮ");
        saveBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 12 60;");
        saveBtn.setOnAction(ev -> {
            String type = switch(typeC.getValue()) {
                case "На перервах" -> "BREAKS";
                case "У конкретний час" -> "TIME";
                case "Разово" -> "ONCE";
                default -> "BREAKS";
            };
            String anchor = switch(breakAnchorC.getValue()) {
                case "Початок перерви" -> "START";
                case "Кінець перерви" -> "END";
                case "Середина перерви" -> "MIDDLE";
                case "Зі зміщенням (хв)" -> "OFFSET";
                default -> "START";
            };
            int offset = 0;
            try { offset = Integer.parseInt(offsetF.getText().trim()); } catch (Exception ignored) {}

            boolean isFolder = pathF.getText().equals(event != null ? event.path() : "") ? (event != null && event.isFolder()) : new File(pathF.getText()).isDirectory();
            MediaEvent newEv = new MediaEvent(event != null ? event.id() : null, nameF.getText(), pathF.getText(), type, timeF.getText(), "1,2,3,4,5", dateP.getValue().toString(), true, isFolder, 0, anchor, offset);
            
            if (event == null) mainApp.getMediaSchedulerService().addEvent(newEv);
            else mainApp.getMediaSchedulerService().updateEvent(newEv);
            
            mainApp.showNotifications();
            stage.close();
        });

        HBox footer = new HBox(saveBtn);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(header, grid, dynamicFields, footer);
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add("data:text/css," + MODERN_DATE_PICKER_STYLE.replace(" ", "%20"));
        stage.setScene(scene);
        stage.showAndWait();
    }
}
