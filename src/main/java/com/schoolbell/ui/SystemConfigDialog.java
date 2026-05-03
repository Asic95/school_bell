package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.File;
import java.util.List;

import static com.schoolbell.ui.UIStyles.*;

public class SystemConfigDialog {
    private final MainApp mainApp;
    
    // Icons from MainApp
    private static final String ICON_BELL = "M12,2A2,2 0 0,0 10,4A2,2 0 0,0 10,4.29C7.12,5.14 5,7.82 5,11V17L3,19V20H21V19L19,17V11C19,7.82 16.88,5.14 14,4.29C14,4.19 14,4.1 14,4A2,2 0 0,0 12,2M10,21A2,2 0 0,0 12,23A2,2 0 0,0 14,21H10Z";
    private static final String ICON_MEGAPHONE = "M12,18H10L9,21H7L8,18H5C3.9,18 3,17.1 3,16V9C3,7.9 3.9,7 5,7H11L18,2V21L11,16M11,14V9H5V16H8.4L11,14Z";
    private static final String ICON_ALERT = "M13,14H11V9H13M13,18H11V16H13M1,21H23L12,2L1,21Z";
    private static final String ICON_SETTINGS = "M12,15.5A3.5,3.5 0 0,1 8.5,12A3.5,3.5 0 0,1 12,8.5A3.5,3.5 0 0,1 15.5,12A3.5,3.5 0 0,1 12,15.5M19.43,12.97C19.47,12.65 19.5,12.33 19.5,12C19.5,11.67 19.47,11.35 19.43,11.03L21.54,9.37C21.73,9.22 21.78,8.95 21.66,8.73L19.66,5.27C19.54,5.05 19.27,4.97 19.05,5.05L16.56,5.9C16.04,5.5 15.48,5.19 14.88,4.97L14.5,2.33C14.46,2.1 14.26,1.92 14.03,1.92H10.03C9.8,1.92 9.6,2.1 9.57,2.33L9.18,4.97C8.58,5.19 8.02,5.5 7.5,5.9L5.01,5.05C4.79,4.97 4.52,5.05 4.4,5.27L2.4,8.73C2.28,8.95 2.33,9.22 2.52,9.37L4.63,11.03C4.59,11.35 4.56,11.67 4.56,12C4.56,12.33 4.59,12.65 4.63,12.97L2.52,14.63C2.33,14.78 2.28,15.05 2.4,15.27L4.4,18.73C4.52,18.95 4.79,19.03 5.01,18.95L7.5,18.1C8.02,18.5 8.58,18.81 9.18,19.03L9.57,21.67C9.6,21.9 9.8,22.08 10.03,22.08H14.03C14.26,22.08 14.46,21.9 14.5,21.67L14.88,19.03C15.48,18.81 16.04,18.5 16.56,18.1L19.05,18.95C19.27,19.03 19.54,18.95 19.66,18.73L21.66,15.27C21.78,15.05 21.73,14.78 21.54,14.63L19.43,12.97Z";
    private static final String ICON_MUSIC = "M21,3V15.5A3.5,3.5 0 0,1 17.5,19A3.5,3.5 0 0,1 14,15.5A3.5,3.5 0 0,1 17.5,12C18.04,12 18.55,12.12 19,12.34V6.47L9,8.6V17.5A3.5,3.5 0 0,1 5.5,21A3.5,3.5 0 0,1 2,17.5A3.5,3.5 0 0,1 5.5,14C6.04,14 6.55,14.12 7,14.34V4.53L21,2V3Z";
    private static final String ICON_FOLDER = "M10,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V8C22,6.89 21.1,6 20,6H12L10,4Z";
    private static final String ICON_SAVE = "M17,3L21,7V19A2,2 0 0,1 19,21H5C3.89,21 3,20.1 3,19V5A2,2 0 0,1 5,3H17M12,12A3,3 0 0,0 9,15A3,3 0 0,0 12,18A3,3 0 0,0 15,15A3,3 0 0,0 12,12M15,5H5V9H15V5Z";

    public SystemConfigDialog(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void show(Stage owner) {
        Stage dialog = new Stage(); dialog.initModality(Modality.WINDOW_MODAL); dialog.initOwner(owner); dialog.setTitle("Конфігурація системи");
        VBox root = new VBox(15); root.setPadding(new Insets(20)); root.setStyle("-fx-background-color: #f1f2f6;");
        TabPane tabPane = new TabPane();
        tabPane.getStylesheets().add("data:text/css," + TAB_STYLE);
        
        Tab relayTab = new Tab("🔔 СИГНАЛИ РЕЛЕ"); relayTab.setClosable(false);
        VBox relayContent = new VBox(15); relayContent.setPadding(new Insets(15)); relayContent.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 15 15;");
        VBox sec1 = createSettingsSection("СТАНДАРТНИЙ ДЗВІНОК", "#0984e3", ICON_BELL);
        TextField regField = createStyledField(String.valueOf(mainApp.getRegularBellDuration()));
        HBox prev1 = new HBox(); prev1.setAlignment(Pos.CENTER); updatePreview(prev1, List.of(mainApp.getRegularBellDuration()), 0, "#0984e3");
        regField.textProperty().addListener((o, ov, nv) -> updatePreview(prev1, parseSafe(nv), 0, "#0984e3"));
        sec1.getChildren().addAll(createFieldRow("Тривалість (сек):", regField), prev1);
        
        VBox sec2 = createSettingsSection("ПОВІТРЯНА ТРИВОГА", "#f39c12", ICON_MEGAPHONE);
        TextField arRingField = createStyledField(String.valueOf(mainApp.getAirRaidRingDuration()));
        TextField arPauseField = createStyledField(String.valueOf(mainApp.getAirRaidPauseDuration()));
        HBox prev2 = new HBox(); prev2.setAlignment(Pos.CENTER); updatePreview(prev2, List.of(mainApp.getAirRaidRingDuration(), mainApp.getAirRaidRingDuration(), mainApp.getAirRaidRingDuration()), mainApp.getAirRaidPauseDuration(), "#f39c12");
        arRingField.textProperty().addListener((o, ov, nv) -> updatePreview(prev2, List.of(parseSafe(nv).get(0), parseSafe(nv).get(0), parseSafe(nv).get(0)), parseSafe(arPauseField.getText()).get(0), "#f39c12"));
        arPauseField.textProperty().addListener((o, ov, nv) -> updatePreview(prev2, List.of(parseSafe(arRingField.getText()).get(0), parseSafe(arRingField.getText()).get(0), parseSafe(arRingField.getText()).get(0)), parseSafe(nv).get(0), "#f39c12"));
        sec2.getChildren().addAll(createFieldRow("Сигнал (сек):", arRingField), createFieldRow("Пауза (сек):", arPauseField), prev2);
        
        VBox sec3 = createSettingsSection("НАДЗВИЧАЙНА СИТУАЦІЯ", "#d63031", ICON_ALERT);
        TextField emField = createStyledField(String.valueOf(mainApp.getEmergencyDuration()));
        HBox prev3 = new HBox(); prev3.setAlignment(Pos.CENTER); updatePreview(prev3, List.of(mainApp.getEmergencyDuration()), 0, "#d63031");
        emField.textProperty().addListener((o, ov, nv) -> updatePreview(prev3, parseSafe(nv), 0, "#d63031"));
        sec3.getChildren().addAll(createFieldRow("Тривалість (сек):", emField), prev3);
        relayContent.getChildren().addAll(sec1, sec2, sec3); 
        relayTab.setContent(relayContent);

        Tab notifyTab = new Tab("🔊 СПОВІЩЕННЯ"); notifyTab.setClosable(false);
        VBox notifyContent = new VBox(15); notifyContent.setPadding(new Insets(15)); notifyContent.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 15 15;");
        VBox deviceSec = createSettingsSection("АУДІО ВИХІД", "#00b894", ICON_SETTINGS);
        ComboBox<String> deviceCombo = new ComboBox<>(); deviceCombo.getItems().add("Системний за замовчуванням");
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (AudioSystem.getMixer(info).isLineSupported(new DataLine.Info(SourceDataLine.class, new AudioFormat(44100, 16, 2, true, false)))) { deviceCombo.getItems().add(info.getName()); }
        }
        deviceCombo.setValue(mainApp.getSelectedAudioDeviceName()); deviceCombo.setMaxWidth(Double.MAX_VALUE); deviceCombo.setStyle(COMBO_STYLE);
        deviceSec.getChildren().add(deviceCombo);
        
        VBox audioSec = createSettingsSection("ФАЙЛИ СПОВІЩЕНЬ", "#6c5ce7", ICON_MUSIC);
        VBox arBox = createAudioFileRow("Повітряна тривога", mainApp.getAudioAirRaidPath(), mainApp::setAudioAirRaidPath, mainApp.isAudioAirRaidEnabled(), mainApp::setAudioAirRaidEnabled, dialog);
        VBox emBox = createAudioFileRow("Надзвичайна ситуація", mainApp.getAudioEmergencyPath(), mainApp::setAudioEmergencyPath, mainApp.isAudioEmergencyEnabled(), mainApp::setAudioEmergencyEnabled, dialog);
        VBox siBox = createAudioFileRow("Хвилина мовчання (09:00)", mainApp.getAudioSilencePath(), mainApp::setAudioSilencePath, mainApp.isAudioSilenceEnabled(), mainApp::setAudioSilenceEnabled, dialog);
        audioSec.getChildren().addAll(arBox, emBox, siBox);
        notifyContent.getChildren().addAll(deviceSec, audioSec); 
        notifyTab.setContent(notifyContent);

        tabPane.getTabs().addAll(relayTab, notifyTab);
        Button saveBtn = new Button("ЗБЕРЕГТИ ВСІ ЗМІНИ"); saveBtn.setGraphic(createSVGIcon(ICON_SAVE, Color.WHITE, 18)); saveBtn.setGraphicTextGap(10);
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 60; -fx-background-radius: 12;");
        saveBtn.setOnAction(e -> { try {
            mainApp.setRegularBellDuration(Integer.parseInt(regField.getText()));
            mainApp.setAirRaidRingDuration(Integer.parseInt(arRingField.getText()));
            mainApp.setAirRaidPauseDuration(Integer.parseInt(arPauseField.getText()));
            mainApp.setEmergencyDuration(Integer.parseInt(emField.getText()));
            mainApp.setAudioAirRaidEnabled(((CheckBox)arBox.getChildren().get(0)).isSelected());
            mainApp.setAudioEmergencyEnabled(((CheckBox)emBox.getChildren().get(0)).isSelected());
            mainApp.setAudioSilenceEnabled(((CheckBox)siBox.getChildren().get(0)).isSelected());
            mainApp.setSelectedAudioDeviceName(deviceCombo.getValue());
            mainApp.saveConfig(); mainApp.addLog("Налаштування оновлено.", "SUCCESS"); dialog.close();
        } catch (Exception ex) { new Alert(Alert.AlertType.ERROR, "Помилка даних!").show(); } });
        
        HBox footer = new HBox(saveBtn); footer.setAlignment(Pos.CENTER);
        root.getChildren().addAll(tabPane, footer); dialog.setScene(new Scene(root, 650, 920)); dialog.showAndWait();
    }

    private VBox createAudioFileRow(String labelText, String currentPath, java.util.function.Consumer<String> onPathSelected, boolean enabled, java.util.function.Consumer<Boolean> onEnabledToggled, Stage owner) {
        CheckBox cb = new CheckBox(labelText); cb.setSelected(enabled);
        TextField pathF = new TextField(currentPath); pathF.setEditable(false); pathF.setPromptText("Файл не обрано");
        pathF.setStyle("-fx-font-size: 12px; -fx-background-color: #f8f9fa; -fx-background-insets: 0; -fx-border-color: #dfe6e9; -fx-border-radius: 8 0 0 8; -fx-background-radius: 8 0 0 8; -fx-padding: 8; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        HBox.setHgrow(pathF, Priority.ALWAYS);
        Button browseBtn = new Button("ОБРАТИ ФАЙЛ");
        browseBtn.setGraphic(createSVGIcon(ICON_FOLDER, Color.WHITE, 16));
        browseBtn.setGraphicTextGap(8);
        browseBtn.setStyle("-fx-background-color: #636e72; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 0 8 8 0; -fx-padding: 8 20;");
        browseBtn.setOnAction(e -> { File f = new FileChooser().showOpenDialog(owner); if (f != null) { pathF.setText(f.getAbsolutePath()); onPathSelected.accept(f.getAbsolutePath()); } });
        HBox row = new HBox(0, pathF, browseBtn); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(0, 0, 5, 25));
        return new VBox(5, cb, row);
    }

    private void updatePreview(HBox box, List<Integer> rings, int pause, String hexColor) {
        box.getChildren().clear(); box.setPadding(new Insets(5, 0, 5, 0)); double scale = 25.0;
        for (int i = 0; i < rings.size(); i++) {
            int d = rings.get(i);
            if (d > 0) {
                VBox block = new VBox(3); block.setAlignment(Pos.CENTER);
                Rectangle r = new Rectangle(Math.max(25, d * scale), 48); r.setArcWidth(12); r.setArcHeight(12); r.setFill(Color.web(hexColor));
                Label l = new Label(d + "с"); l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
                block.getChildren().addAll(r, l); box.getChildren().add(block);
            }
            if (i < rings.size() - 1 && pause > 0) {
                VBox pBlock = new VBox(3); pBlock.setAlignment(Pos.CENTER);
                Rectangle p = new Rectangle(Math.max(15, pause * scale), 48); p.setFill(Color.web("#f1f2f6"));
                p.setStroke(Color.web("#bdc3c7")); p.getStrokeDashArray().addAll(5.0, 5.0);
                Label l = new Label(pause + "с"); l.setStyle("-fx-font-size: 11px; -fx-text-fill: #bdc3c7;");
                pBlock.getChildren().addAll(p, l); box.getChildren().add(pBlock);
            }
        }
    }

    private VBox createSettingsSection(String title, String color, String svgPath) {
        VBox v = new VBox(8); v.setPadding(new Insets(12)); v.setStyle(DEPTH_2);
        HBox header = new HBox(12, createSVGIcon(svgPath, Color.web(color), 18), new Label(title));
        header.setAlignment(Pos.CENTER_LEFT); ((Label)header.getChildren().get(1)).setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3436;");
        v.getChildren().add(header); return v;
    }

    private HBox createFieldRow(String label, TextField field) {
        HBox h = new HBox(10, new Label(label), new Region(), field); HBox.setHgrow(h.getChildren().get(1), Priority.ALWAYS);
        h.setAlignment(Pos.CENTER_LEFT); return h;
    }

    private TextField createStyledField(String val) {
        TextField f = new TextField(val); f.setPrefWidth(70); f.setAlignment(Pos.CENTER);
        f.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 6; -fx-border-color: #dfe6e9; -fx-border-radius: 6; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 4 7;"); return f;
    }

    private List<Integer> parseSafe(String val) { try { return List.of(Math.max(0, Integer.parseInt(val))); } catch (Exception e) { return List.of(0); } }
}
