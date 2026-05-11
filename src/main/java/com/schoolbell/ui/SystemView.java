package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.sound.sampled.*;
import java.io.File;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class SystemView {
    private final MainApp mainApp;
    private final ConfigService config;

    // Identity
    private final TextField schoolNameField;
    private final TextField cityNameField;

    // Hardware
    private final TextField regField;
    private final TextField arRingField;
    private final TextField arPauseField;
    private final TextField emField;
    private final CheckBox simulationModeCb;

    // Audio
    private ComboBox<String> audioDeviceCombo;
    private final Slider volumeSlider;
    private final CheckBox arAudioCb;
    private final TextField arAudioPath;
    private final CheckBox emAudioCb;
    private final TextField emAudioPath;
    private final CheckBox siAudioCb;
    private final TextField siAudioPath;

    // Network
    private final TextField portField;

    public SystemView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();

        // Identity
        schoolNameField = createStyledField(config.getSchoolName());
        cityNameField = createStyledField(config.getCityName());

        // Hardware
        regField = createStyledField(String.valueOf(config.getRegularBellDuration()));
        arRingField = createStyledField(String.valueOf(config.getAirRaidRingDuration()));
        arPauseField = createStyledField(String.valueOf(config.getAirRaidPauseDuration()));
        emField = createStyledField(String.valueOf(config.getEmergencyDuration()));
        simulationModeCb = new CheckBox("РЕЖИМ СИМУЛЯЦІЇ (без фізичного реле)");
        simulationModeCb.setSelected(config.isSimulationMode());
        simulationModeCb.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_PRIMARY + ";");

        // Audio
        volumeSlider = new Slider(0, 100, config.getSystemVolume());
        arAudioCb = new CheckBox("Аудіо для тривоги");
        arAudioCb.setSelected(config.isAudioAirRaidEnabled());
        arAudioPath = new TextField(config.getAudioAirRaidPath());
        
        emAudioCb = new CheckBox("Аудіо для НС");
        emAudioCb.setSelected(config.isAudioEmergencyEnabled());
        emAudioPath = new TextField(config.getAudioEmergencyPath());
        
        siAudioCb = new CheckBox("Хвилина мовчання (09:00)");
        siAudioCb.setSelected(config.isAudioSilenceEnabled());
        siAudioPath = new TextField(config.getAudioSilencePath());

        // Network
        portField = createStyledField(String.valueOf(config.getBroadcastPort()));
    }

    public Node build() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ВСЕ", ICON_SAVE);
        saveBtn.setOnAction(e -> save());

        VBox headerArea = createSectionHeader(
                "Системні налаштування",
                "Конфігурація обладнання, звуку та мережевих параметрів",
                "#2d3436",
                ICON_SETTINGS,
                saveBtn
        );

        TabPane tabPane = new TabPane();
        tabPane.setStyle(TAB_STYLE);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        tabPane.getTabs().addAll(
                createTab("АПАРАТУРА", ICON_BELL, buildHardwareTab()),
                createTab("ЗВУК", ICON_VOLUME, buildAudioTab()),
                createTab("СИСТЕМА ТА МЕРЕЖА", ICON_BROADCAST, buildSystemTab())
        );

        root.getChildren().addAll(headerArea, tabPane);
        
        return root;
    }

    private Tab createTab(String title, String icon, Node content) {
        Tab tab = new Tab(title);
        tab.setGraphic(createSVGIcon(icon, Color.web(COLOR_TEXT_DIM), 14));
        tab.setContent(content);
        return tab;
    }

    private Node buildHardwareTab() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16 16;");

        VBox durationsCard = createSettingsSection("ТРИВАЛІСТЬ СИГНАЛІВ", COLOR_PRIMARY, ICON_CLOCK);
        GridPane grid = new GridPane();
        grid.setHgap(30); grid.setVgap(20);
        grid.add(new Label("Стандартний дзвінок (сек):"), 0, 0); grid.add(regField, 1, 0);
        grid.add(new Label("Тривога: звук (сек):"), 0, 1); grid.add(arRingField, 1, 1);
        grid.add(new Label("Тривога: пауза (сек):"), 0, 2); grid.add(arPauseField, 1, 2);
        grid.add(new Label("Екстрений сигнал (сек):"), 0, 3); grid.add(emField, 1, 3);
        durationsCard.getChildren().add(grid);

        VBox diagCard = createSettingsSection("ДІАГНОСТИКА РЕЛЕ", COLOR_SUCCESS, ICON_SETTINGS);
        Label relayStatus = new Label("Статус: " + (mainApp.getRelayController().isConnected() ? "ПІДКЛЮЧЕНО" : "НЕМАЄ ЗВ'ЯЗКУ"));
        relayStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (mainApp.getRelayController().isConnected() ? COLOR_SUCCESS : COLOR_DANGER) + ";");
        
        Button testRelay = new Button("ТЕСТ РЕЛЕ (1 СЕК)");
        testRelay.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_TEXT + "; -fx-padding: 8 20;");
        testRelay.setOnAction(e -> mainApp.getSignalService().testRelay());
        
        diagCard.getChildren().addAll(relayStatus, simulationModeCb, testRelay);

        content.getChildren().addAll(durationsCard, diagCard);
        return new ScrollPane(content);
    }

    private Node buildAudioTab() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16 16;");

        VBox outputCard = createSettingsSection("ВИХІД ТА ГУЧНІСТЬ", COLOR_SUCCESS, ICON_VOLUME);
        audioDeviceCombo = new ComboBox<>();
        audioDeviceCombo.getItems().add("За замовчуванням");
        try {
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                if (AudioSystem.getMixer(info).isLineSupported(new DataLine.Info(SourceDataLine.class, new AudioFormat(44100, 16, 2, true, false)))) {
                    audioDeviceCombo.getItems().add(info.getName());
                }
            }
        } catch (Exception e) {}
        audioDeviceCombo.setValue(config.getSelectedAudioDeviceName());
        audioDeviceCombo.setStyle(COMBO_STYLE);
        audioDeviceCombo.setMaxWidth(400);

        Label volVal = new Label((int)volumeSlider.getValue() + "%");
        volVal.setStyle("-fx-font-weight: bold;");
        volumeSlider.valueProperty().addListener((o, ov, nv) -> volVal.setText(nv.intValue() + "%"));
        
        HBox volRow = new HBox(15, volumeSlider, volVal);
        volRow.setAlignment(Pos.CENTER_LEFT);

        Button testAudio = new Button("ТЕСТ ЗВУКУ");
        testAudio.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 8 20;");
        testAudio.setOnAction(e -> java.awt.Toolkit.getDefaultToolkit().beep());

        Label infoLabel = new Label("Додаткові налаштування файлів та табло перенесено у розділ \"Сповіщення\"");
        infoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-padding: 10 0 0 0;");

        outputCard.getChildren().addAll(
            new Label("Пристрій відтворення:"), 
            audioDeviceCombo, 
            new Label("Загальна гучність:"), 
            volRow, 
            testAudio,
            infoLabel
        );

        content.getChildren().add(outputCard);
        return new ScrollPane(content);
    }

    private Node buildSystemTab() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16 16;");

        VBox identityCard = createSettingsSection("ПРОФІЛЬ ЗАКЛАДУ", COLOR_PRIMARY, ICON_PERSON);
        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(15);
        grid.add(new Label("Назва школи:"), 0, 0); grid.add(schoolNameField, 1, 0);
        grid.add(new Label("Місто:"), 0, 1); grid.add(cityNameField, 1, 1);
        schoolNameField.setPrefWidth(400);
        identityCard.getChildren().add(grid);

        VBox netCard = createSettingsSection("МЕРЕЖА", "#6c5ce7", ICON_BROADCAST);
        netCard.getChildren().addAll(
            new Label("Порт веб-табло:"),
            portField
        );
        portField.setMaxWidth(100);

        content.getChildren().addAll(identityCard, netCard);
        return new ScrollPane(content);
    }

    private Node createAudioRow(CheckBox cb, TextField path, String label) {
        path.setEditable(false);
        path.setStyle(FIELD_STYLE + "-fx-background-color: #f1f2f6;");
        HBox.setHgrow(path, Priority.ALWAYS);
        
        Button browse = new Button("...");
        browse.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_TEXT_DIM + "; -fx-padding: 5 15;");
        browse.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(mainApp.getStage());
            if (f != null) path.setText(f.getAbsolutePath());
        });

        HBox row = new HBox(10, path, browse);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 10, 25));
        
        return new VBox(5, cb, row);
    }

    private void save() {
        try {
            config.setSchoolName(schoolNameField.getText());
            config.setCityName(cityNameField.getText());
            config.setRegularBellDuration(Integer.parseInt(regField.getText()));
            config.setAirRaidRingDuration(Integer.parseInt(arRingField.getText()));
            config.setAirRaidPauseDuration(Integer.parseInt(arPauseField.getText()));
            config.setEmergencyDuration(Integer.parseInt(emField.getText()));
            config.setSimulationMode(simulationModeCb.isSelected());
            config.setSystemVolume((int) volumeSlider.getValue());
            config.setSelectedAudioDeviceName(audioDeviceCombo.getValue());
            config.setAudioAirRaidEnabled(arAudioCb.isSelected());
            config.setAudioAirRaidPath(arAudioPath.getText());
            config.setAudioEmergencyEnabled(emAudioCb.isSelected());
            config.setAudioEmergencyPath(emAudioPath.getText());
            config.setAudioSilenceEnabled(siAudioCb.isSelected());
            config.setAudioSilencePath(siAudioPath.getText());
            config.setBroadcastPort(Integer.parseInt(portField.getText()));

            mainApp.saveConfig();
            mainApp.addLog("Всі системні налаштування збережено", "SUCCESS");
            
            ToastService.showSuccess("Налаштування збережено!");
        } catch (Exception e) {
            ToastService.showError("Помилка при збереженні: " + e.getMessage());
        }
    }
}
