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

public class NotificationsView {
    private final MainApp mainApp;
    private final ConfigService config;
    private ComboBox<String> deviceCombo;
    private VBox arBox;
    private VBox emBox;
    private VBox siBox;

    public NotificationsView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
    }

    public Node build() {
        VBox root = new VBox(35);
        root.setPadding(new Insets(35));
        root.setStyle("-fx-background-color: #f1f2f6;");

        // --- SAVE BUTTON (Header Action) ---
        Button saveBtn = createPrimaryActionButton("Зберегти налаштування", ICON_SAVE);
        saveBtn.setOnAction(e -> save());

        // --- TOP HEADER (Standardized with Action) ---
        VBox headerArea = createSectionHeader(
                "Аудіо сповіщення",
                "Керування звуковими файлами та вибором обладнання для трансляції",
                "#2d3436",
                ICON_MUSIC,
                saveBtn
        );
        root.getChildren().add(headerArea);

        // --- MAIN LAYOUT ---
        HBox mainLayout = new HBox(45);
        mainLayout.setAlignment(Pos.TOP_LEFT);
        
        VBox leftSide = new VBox(30);
        HBox.setHgrow(leftSide, Priority.ALWAYS);

        // --- SECTION 1: AUDIO DEVICE ---
        VBox deviceCard = createModernConfigCard("АУДІО ВИХІД", "Оберіть пристрій для відтворення звуку", ICON_SETTINGS, "#00b894");
        deviceCombo = new ComboBox<>();
        deviceCombo.getItems().add("Системний за замовчуванням");
        try {
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                if (AudioSystem.getMixer(info).isLineSupported(new DataLine.Info(SourceDataLine.class, new AudioFormat(44100, 16, 2, true, false)))) {
                    deviceCombo.getItems().add(info.getName());
                }
            }
        } catch (Exception e) {}
        deviceCombo.setValue(config.getSelectedAudioDeviceName());
        deviceCombo.setMaxWidth(Double.MAX_VALUE);
        deviceCombo.setStyle(COMBO_STYLE);
        deviceCard.getChildren().add(deviceCombo);

        // --- SECTION 2: AUDIO FILES ---
        VBox audioSec = createModernConfigCard("ФАЙЛИ СПОВІЩЕНЬ", "Налаштування аудіо для екстрених режимів", ICON_MUSIC, "#6c5ce7");
        arBox = createAudioFileRow("Повітряна тривога", config.getAudioAirRaidPath(), config.isAudioAirRaidEnabled());
        emBox = createAudioFileRow("Надзвичайна ситуація", config.getAudioEmergencyPath(), config.isAudioEmergencyEnabled());
        siBox = createAudioFileRow("Хвилина мовчання (09:00)", config.getAudioSilencePath(), config.isAudioSilenceEnabled());
        audioSec.getChildren().addAll(arBox, emBox, siBox);

        leftSide.getChildren().addAll(deviceCard, audioSec);

        // --- RIGHT SIDE: HELP PANEL (Unified) ---
        VBox helpPanel = createSideHelpPanel(
            createHelpCard(ICON_SETTINGS, "Вибір пристрою", "Якщо до ПК підключено декілька динаміків (наприклад, HDMI та лінійний вихід), оберіть потрібний у списку.", "#00b894"),
            createHelpCard(ICON_MUSIC, "Формати файлів", "Система найкраще працює з форматами .MP3 та .WAV. Рекомендується використовувати якісні записи.", "#6c5ce7"),
            createHelpCard(ICON_CLOCK, "Хвилина мовчання", "Аудіофайл для хвилини мовчання буде автоматично запущено рівно о 09:00 кожного дня.", "#fdcb6e")
        );

        mainLayout.getChildren().addAll(leftSide, helpPanel);
        root.getChildren().add(mainLayout);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private VBox createModernConfigCard(String title, String subtitle, String iconPath, String color) {
        VBox card = new VBox(20);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 28; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 15, 0, 0, 5);");
        
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(color), 32));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(64, 64);
        iconBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 18;");
        
        VBox texts = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #2d3436;");
        Label s = new Label(subtitle.toUpperCase());
        s.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 1px;");
        texts.getChildren().addAll(t, s);
        
        header.getChildren().addAll(iconBox, texts);
        card.getChildren().add(header);
        
        return card;
    }

    private void save() {
        config.setSelectedAudioDeviceName(deviceCombo.getValue());
        
        CheckBox arCb = (CheckBox) arBox.getChildren().get(0);
        TextField arTf = (TextField) ((HBox) arBox.getChildren().get(1)).getChildren().get(0);
        config.setAudioAirRaidEnabled(arCb.isSelected());
        config.setAudioAirRaidPath(arTf.getText());

        CheckBox emCb = (CheckBox) emBox.getChildren().get(0);
        TextField emTf = (TextField) ((HBox) emBox.getChildren().get(1)).getChildren().get(0);
        config.setAudioEmergencyEnabled(emCb.isSelected());
        config.setAudioEmergencyPath(emTf.getText());

        CheckBox siCb = (CheckBox) siBox.getChildren().get(0);
        TextField siTf = (TextField) ((HBox) siBox.getChildren().get(1)).getChildren().get(0);
        config.setAudioSilenceEnabled(siCb.isSelected());
        config.setAudioSilencePath(siTf.getText());

        mainApp.saveConfig();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успіх");
        alert.setHeaderText(null);
        alert.setContentText("Налаштування сповіщень збережено!");
        alert.showAndWait();
    }

    private VBox createAudioFileRow(String labelText, String currentPath, boolean enabled) {
        CheckBox cb = new CheckBox(labelText);
        cb.setSelected(enabled);
        cb.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + ";");
        
        TextField pathF = new TextField(currentPath);
        pathF.setEditable(false);
        pathF.setPromptText("Файл не обрано");
        pathF.setStyle("-fx-font-size: 13px; -fx-background-color: #f8f9fa; -fx-background-insets: 0; -fx-border-color: #dfe6e9; -fx-border-radius: 10 0 0 10; -fx-background-radius: 10 0 0 10; -fx-padding: 10;");
        HBox.setHgrow(pathF, Priority.ALWAYS);
        
        Button browseBtn = new Button("ОБРАТИ ФАЙЛ");
        browseBtn.setGraphic(createSVGIcon(ICON_FOLDER, Color.WHITE, 16));
        browseBtn.setGraphicTextGap(8);
        browseBtn.setStyle("-fx-background-color: #636e72; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 0 10 10 0; -fx-padding: 10 20; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav"));
            File f = fc.showOpenDialog(mainApp.getStage());
            if (f != null) {
                pathF.setText(f.getAbsolutePath());
            }
        });
        
        HBox row = new HBox(0, pathF, browseBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 0, 10, 25));
        
        return new VBox(5, cb, row);
    }
}
