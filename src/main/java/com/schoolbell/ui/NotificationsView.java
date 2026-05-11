package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.File;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class NotificationsView {
    private final MainApp mainApp;
    private final ConfigService config;

    private ComboBox<String> deviceCombo;
    private Slider volumeSlider;

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
        VBox card = new VBox(20);
        card.setPadding(new Insets(25));
        card.setStyle(SOFT_CARD);

        HBox top = new HBox(40);
        top.setAlignment(Pos.CENTER_LEFT);

        deviceCombo = new ComboBox<>();
        VBox devBox = new VBox(8, new Label("ПРИСТРІЙ ВІДТВОРЕННЯ"), deviceCombo);
        HBox.setHgrow(devBox, Priority.ALWAYS);
        ((Label)devBox.getChildren().get(0)).setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        deviceCombo.setMaxWidth(Double.MAX_VALUE);
        deviceCombo.setStyle(COMBO_STYLE);
        deviceCombo.setValue(config.getSelectedAudioDeviceName());
        try {
            deviceCombo.getItems().add("Системний за замовчуванням");
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                if (AudioSystem.getMixer(info).isLineSupported(new DataLine.Info(SourceDataLine.class, new AudioFormat(44100, 16, 2, true, false)))) {
                    deviceCombo.getItems().add(info.getName());
                }
            }
        } catch (Exception ignored) {}

        volumeSlider = new Slider(0, 100, config.getSystemVolume());
        Label volVal = new Label(config.getSystemVolume() + "%");
        volVal.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 18px;");
        volumeSlider.valueProperty().addListener((o, ov, nv) -> volVal.setText(nv.intValue() + "%"));
        
        VBox volBox = new VBox(8, new Label("ГУЧНІСТЬ СПОВІЩЕНЬ"), new HBox(15, volumeSlider, volVal));
        ((Label)volBox.getChildren().get(0)).setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        ((HBox)volBox.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);

        top.getChildren().addAll(devBox, volBox);
        card.getChildren().add(top);
        return card;
    }

    private VBox createAlertRow(String title, String icon, String color, ToggleButton audioTg, TextField pathField, ToggleButton visualTg, String alertType, boolean showSeparator) {
        VBox root = new VBox();
        HBox row = new HBox(30);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(25));
        row.setStyle("-fx-background-color: white;");

        // 1. IDENTITY & TYPE
        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(color), 24));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(50, 50);
        iconBox.setMinSize(50, 50);
        iconBox.setStyle("-fx-background-color: " + color + "10; -fx-background-radius: 14;");
        
        VBox titleBox = new VBox(2, new Label("ТИП СИГНАЛУ"), new Label(title));
        ((Label)titleBox.getChildren().get(0)).setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        ((Label)titleBox.getChildren().get(1)).setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #2d3436;");
        titleBox.setMinWidth(180);

        // 2. AUDIO SETTINGS (EXPANDABLE)
        VBox audioCol = new VBox(10);
        HBox.setHgrow(audioCol, Priority.ALWAYS);
        
        HBox aHead = new HBox(12, audioTg, new Label("АУДІО-СУПРОВІД"));
        aHead.setAlignment(Pos.CENTER_LEFT);
        ((Label)aHead.getChildren().get(1)).setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #636e72;");
        
        HBox pathRow = new HBox(10);
        pathField.setEditable(false);
        pathField.setPromptText("Шлях до файлу...");
        pathField.setStyle(FIELD_STYLE + "-fx-background-color: #f8f9fa; -fx-font-size: 12px; -fx-text-fill: #2d3436;");
        HBox.setHgrow(pathField, Priority.ALWAYS);
        
        Button browse = new Button("ОБРАТИ");
        browse.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_TEXT_DIM + "; -fx-font-size: 9px; -fx-padding: 6 12;");
        browse.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Оберіть аудіофайл");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо файли (MP3, WAV)", "*.mp3", "*.wav"));
            File f = fc.showOpenDialog(mainApp.getStage());
            if (f != null) pathField.setText(f.getAbsolutePath());
        });
        pathRow.getChildren().addAll(pathField, browse);
        audioCol.getChildren().addAll(aHead, pathRow);

        // 3. VISUAL SETTINGS (COMPACT)
        VBox visualCol = new VBox(10);
        visualCol.setAlignment(Pos.CENTER_LEFT);
        visualCol.setMinWidth(140);
        
        HBox vHead = new HBox(12, visualTg, new Label("ТАБЛО"));
        vHead.setAlignment(Pos.CENTER_LEFT);
        ((Label)vHead.getChildren().get(1)).setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #636e72;");
        
        Label vDesc = new Label("Візуальне сповіщення");
        vDesc.setStyle("-fx-font-size: 9px; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-padding: 0 0 0 5;");
        visualCol.getChildren().addAll(vHead, vDesc);

        // 4. TEST ACTIONS
        HBox testActions = new HBox(10);
        testActions.setAlignment(Pos.CENTER_RIGHT);
        
        Button tAudio = createTestIconButton(ICON_VOLUME, COLOR_SUCCESS, () -> {
            if (!pathField.getText().isEmpty()) mainApp.getAudioService().playAudioFile(pathField.getText());
            else java.awt.Toolkit.getDefaultToolkit().beep();
        });
        
        Button tVisual = createTestIconButton(ICON_MONITOR, COLOR_PURPLE, () -> {
            mainApp.getSignalService().setTemporaryAlertType(alertType, 5000);
        });
        
        testActions.getChildren().addAll(tAudio, tVisual);

        row.getChildren().addAll(iconBox, titleBox, audioCol, visualCol, testActions);
        root.getChildren().add(row);
        
        if (showSeparator) {
            Separator sep = new Separator();
            sep.setPadding(new Insets(0, 25, 0, 25));
            sep.setStyle("-fx-opacity: 0.5;");
            root.getChildren().add(sep);
        }
        return root;
    }

    private Button createTestIconButton(String icon, String color, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(createSVGIcon(icon, Color.WHITE, 20));
        btn.setPrefSize(50, 50);
        btn.setStyle(BTN_BASE + "-fx-background-color: " + color + "; -fx-background-radius: 14;");
        btn.setOnAction(e -> action.run());
        
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-opacity: 0.9; -fx-effect: dropshadow(three-pass-box, " + color + "40, 12, 0, 0, 5);"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().split("-fx-opacity")[0]));
        
        return btn;
    }

    private ToggleButton createToggleSwitch(boolean initialState) {
        ToggleButton btn = new ToggleButton();
        btn.setSelected(initialState);
        btn.setPrefSize(40, 22);
        btn.setMinSize(40, 22);
        
        Circle thumb = new Circle(8, Color.WHITE);
        thumb.setEffect(new javafx.scene.effect.DropShadow(3, Color.rgb(0,0,0,0.2)));
        
        StackPane container = new StackPane(thumb);
        container.setPrefSize(40, 22);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(0, 3, 0, 3));
        
        btn.setGraphic(container);
        
        Runnable updateStyle = () -> {
            if (btn.isSelected()) {
                container.setStyle("-fx-background-color: " + COLOR_SUCCESS + "; -fx-background-radius: 20;");
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
                tt.setToX(18);
                tt.play();
            } else {
                container.setStyle("-fx-background-color: #dfe6e9; -fx-background-radius: 20;");
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
                tt.setToX(0);
                tt.play();
            }
        };
        
        btn.setOnAction(e -> updateStyle.run());
        updateStyle.run();
        
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
        return btn;
    }

    private void save() {
        config.setSelectedAudioDeviceName(deviceCombo.getValue());
        config.setSystemVolume((int) volumeSlider.getValue());

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
}
