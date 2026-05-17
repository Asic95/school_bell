package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class SignalsView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final TextField regField;
    private final TextField arRingField;
    private final TextField arPauseField;
    private final TextField emField;

    public SignalsView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();

        regField = createStyledField(String.valueOf(config.getRegularBellDuration()));
        arRingField = createStyledField(String.valueOf(config.getAirRaidRingDuration()));
        arPauseField = createStyledField(String.valueOf(config.getAirRaidPauseDuration()));
        emField = createStyledField(String.valueOf(config.getEmergencyDuration()));
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
                "Сигнали реле",
                "Налаштування тривалості та ритму фізичних сигналів дзвінка",
                "#2d3436",
                ICON_BELL,
                saveBtn
        );
        root.getChildren().add(headerArea);

        // --- MAIN LAYOUT (Content + Help) ---
        HBox mainLayout = new HBox(45);
        mainLayout.setAlignment(Pos.TOP_LEFT);
        
        VBox leftSide = new VBox(30);
        HBox.setHgrow(leftSide, Priority.ALWAYS);

        // --- SECTION CARDS ---
        
        // Section 1: Regular Bell
        VBox card1 = createModernSignalCard("СТАНДАРТНИЙ ДЗВІНОК", "Для автоматичних сигналів", ICON_BELL, "#0984e3");
        HBox prev1 = new HBox(); prev1.setAlignment(Pos.CENTER_LEFT);
        updatePreview(prev1, "ЗВИЧАЙНИЙ", List.of(config.getRegularBellDuration()), 0, "#0984e3");
        regField.textProperty().addListener((o, ov, nv) -> updatePreview(prev1, "ЗВИЧАЙНИЙ", parseSafe(nv), 0, "#0984e3"));
        card1.getChildren().addAll(createModernInputRow("Тривалість дзвінка (сек):", regField), prev1);

        // Section 2: Air Raid
        VBox card2 = createModernSignalCard("ПОВІТРЯНА ТРИВОГА", "Циклічний сигнал небезпеки", ICON_MEGAPHONE, "#f39c12");
        HBox prev2 = new HBox(); prev2.setAlignment(Pos.CENTER_LEFT);
        updatePreview(prev2, "ТРИВОГА", List.of(config.getAirRaidRingDuration(), config.getAirRaidRingDuration(), config.getAirRaidRingDuration()), config.getAirRaidPauseDuration(), "#f39c12");
        arRingField.textProperty().addListener((o, ov, nv) -> {
            int ring = parseSafe(nv).get(0);
            int pause = parseSafe(arPauseField.getText()).get(0);
            updatePreview(prev2, "ТРИВОГА", List.of(ring, ring, ring), pause, "#f39c12");
        });
        arPauseField.textProperty().addListener((o, ov, nv) -> {
            int ring = parseSafe(arRingField.getText()).get(0);
            int pause = parseSafe(nv).get(0);
            updatePreview(prev2, "ТРИВОГА", List.of(ring, ring, ring), pause, "#f39c12");
        });
        card2.getChildren().addAll(
            createModernInputRow("Тривалість звуку (сек):", arRingField),
            createModernInputRow("Тривалість паузи (сек):", arPauseField),
            prev2
        );

        // Section 3: Emergency
        VBox card3 = createModernSignalCard("НАДЗВИЧАЙНА СИТУАЦІЯ", "Екстрений сигнал евакуації", ICON_ALERT, "#d63031");
        HBox prev3 = new HBox(); prev3.setAlignment(Pos.CENTER_LEFT);
        updatePreview(prev3, "ЕКСТРЕНА", List.of(config.getEmergencyDuration()), 0, "#d63031");
        emField.textProperty().addListener((o, ov, nv) -> updatePreview(prev3, "ЕКСТРЕНА", parseSafe(nv), 0, "#d63031"));
        card3.getChildren().addAll(createModernInputRow("Тривалість сигналу (сек):", emField), prev3);

        leftSide.getChildren().addAll(card1, card2, card3);

        // --- SECTION 4: DIAGNOSTICS ---
        VBox diagCard = createModernSignalCard("ДІАГНОСТИКА ТА ТЕСТУВАННЯ", "Перевірка працездатності обладнання", ICON_SETTINGS, "#636e72");
        
        Button testRelayBtn = new Button("ТЕСТ РЕЛЕ (1 СЕК)");
        testRelayBtn.setStyle(BTN_BASE + "-fx-background-color: #2d3436; -fx-padding: 10 20;");
        testRelayBtn.setGraphic(createSVGIcon(ICON_BELL, Color.WHITE, 16));
        testRelayBtn.setOnAction(e -> mainApp.getSignalService().testRelay());

        Button testAudioBtn = new Button("ТЕСТ ЗВУКУ");
        testAudioBtn.setStyle(BTN_BASE + "-fx-background-color: #636e72; -fx-padding: 10 20;");
        testAudioBtn.setGraphic(createSVGIcon(ICON_VOLUME, Color.WHITE, 16));
        testAudioBtn.setOnAction(e -> {
            // Play a short system beep or a sample sound if configured
            if (config.isAudioSilenceEnabled() && !config.getAudioSilencePath().isEmpty()) {
                mainApp.getAudioService().playAudioFile(config.getAudioSilencePath());
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        });

        HBox diagActions = new HBox(15, testRelayBtn, testAudioBtn);
        diagActions.setAlignment(Pos.CENTER_LEFT);
        
        Label relayStatus = new Label("Статус реле: " + (mainApp.getRelayController().isConnected() ? "ПІДКЛЮЧЕНО (" + mainApp.getRelayController().getConnectionDetails() + ")" : "НЕ ПІДКЛЮЧЕНО"));
        relayStatus.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (mainApp.getRelayController().isConnected() ? COLOR_SUCCESS : COLOR_DANGER) + ";");

        diagCard.getChildren().addAll(relayStatus, diagActions);
        leftSide.getChildren().add(diagCard);

        // --- RIGHT SIDE: HELP SECTION (Unified Component) ---
        VBox helpPanel = createSideHelpPanel(
            createHelpCard(ICON_BELL, "Автоматичні дзвінки", "Ці налаштування впливають на всі автоматичні сигнали, що подаються за розкладом.", "#0984e3"),
            createHelpCard(ICON_MEGAPHONE, "Ритм тривоги", "Для тривоги використовується 3 цикли. Рекомендується ставити паузу не менше 2-3 секунд.", "#f39c12"),
            createHelpCard(ICON_ALERT, "Гучність та безпека", "Екстрені сигнали мають бути достатньо довгими, щоб їх почули у всіх куточках закладу.", "#d63031")
        );

        mainLayout.getChildren().addAll(leftSide, helpPanel);
        root.getChildren().add(mainLayout);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private VBox createModernSignalCard(String title, String subtitle, String iconPath, String color) {
        VBox card = new VBox(20);
        card.setPadding(new Insets(30));
        card.setStyle(SOFT_CARD + "-fx-padding: 30; -fx-border-color: #f1f2f6; -fx-border-radius: 28;");
        
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(color), 32));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(64, 64);
        iconBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 20;");
        
        VBox texts = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        Label s = new Label(subtitle.toUpperCase());
        s.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 1.5px;");
        texts.getChildren().addAll(t, s);
        
        header.getChildren().addAll(iconBox, texts);
        card.getChildren().add(header);
        
        return card;
    }

    private HBox createModernInputRow(String labelText, TextField field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #636e72;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        field.setPrefWidth(100);
        field.setStyle(FIELD_STYLE + "-fx-font-size: 15px; -fx-font-weight: 900; -fx-padding: 10 15;");
        
        HBox row = new HBox(15, lbl, spacer, field, new Label("СЕК"));
        row.setAlignment(Pos.CENTER_LEFT);
        Label unit = (Label)row.getChildren().get(3);
        unit.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 1px;");
        
        return row;
    }

    private void save() {
        try {
            config.setRegularBellDuration(Integer.parseInt(regField.getText()));
            config.setAirRaidRingDuration(Integer.parseInt(arRingField.getText()));
            config.setAirRaidPauseDuration(Integer.parseInt(arPauseField.getText()));
            config.setEmergencyDuration(Integer.parseInt(emField.getText()));
            mainApp.saveConfig();
            ToastService.showSuccess("Налаштування сигналів збережено!");
            } catch (NumberFormatException e) {
            ToastService.showError("Некоректні дані: будь ласка, введіть числові значення для тривалості.");
            }    }
}
