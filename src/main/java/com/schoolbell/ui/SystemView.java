package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.SystemService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.CardFactory.createHelpCard;
import static com.schoolbell.ui.CardFactory.createSideHelpPanel;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createToggleSwitch;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class SystemView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final SystemService systemService;

    private final ToggleButton autostartTg;
    private final ToggleButton trayTg;
    private final ToggleButton simulationTg;

    private final BellSettingsPane bellSettingsPane;
    private final SystemJournalPane journalPane;
    private VBox mainCol;

    public SystemView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.systemService = mainApp.getSystemService();

        autostartTg = createToggleSwitch(config.isAutostartEnabled());
        trayTg = createToggleSwitch(config.isMinimizeToTray());
        simulationTg = createToggleSwitch(config.isSimulationMode());

        bellSettingsPane = new BellSettingsPane(
                config.getRegularBellDuration(),
                config.getAirRaidRingDuration(),
                config.getAirRaidPauseDuration(),
                config.getEmergencyDuration(),
                true
        );

        journalPane = new SystemJournalPane(mainApp);
        
        simulationTg.selectedProperty().addListener((obs, old, nv) -> updateJournalVisibility(nv));
    }

    public Node build() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ВСЕ", ICON_SAVE);
        saveBtn.setOnAction(e -> save());

        VBox headerArea = createSectionHeader(
                "Системна конфігурація",
                "Глобальні параметри роботи програми, дизайн та поведінка системи",
                "#2d3436",
                ICON_SETTINGS,
                saveBtn
        );

        HBox contentLayout = new HBox(25);
        mainCol = new VBox(25);
        HBox.setHgrow(mainCol, Priority.ALWAYS);

        mainCol.getChildren().add(buildOperationCard());
        mainCol.getChildren().add(buildSignalCard());
        
        updateJournalVisibility(simulationTg.isSelected());

        VBox helpPanel = createSideHelpPanel(
                createHelpCard(ICON_POWER, "Автостарт", "Рекомендуємо увімкнути автостарт, щоб система відновлювалась після вимкнення світла.", COLOR_SUCCESS),
                createHelpCard(ICON_WAVEFORM, "Сигнали", "Тривалості сигналів редагуються в новому візуальному блоці параметрів дзвінків.", COLOR_WARNING)
        );

        contentLayout.getChildren().addAll(mainCol, helpPanel);
        root.getChildren().addAll(headerArea, contentLayout);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scrollPaneStyle(scroll);
        return scroll;
    }

    private void updateJournalVisibility(boolean visible) {
        if (visible) {
            if (!mainCol.getChildren().contains(journalPane)) {
                mainCol.getChildren().add(journalPane);
            }
        } else {
            mainCol.getChildren().remove(journalPane);
        }
    }

    private void scrollPaneStyle(ScrollPane scroll) {
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
    }

    private VBox buildOperationCard() {
        VBox card = createCard("ЗАПУСК ТА РОБОТА", ICON_CLOCK, COLOR_SUCCESS);

        VBox rows = new VBox(8);
        rows.getChildren().add(createToggleRow("Автозапуск разом з Windows",
                "Програма буде автоматично запускатись при вході в систему", ICON_POWER, COLOR_PRIMARY, autostartTg));
        rows.getChildren().add(createToggleRow("Згортати у системний трей",
                "При закритті вікно буде ховатись у трей замість виходу", ICON_TRAY, COLOR_PURPLE, trayTg));
        rows.getChildren().add(createToggleRow("Режим симуляції",
                "Робота без фізичного підключення до реле (тільки логування)", ICON_FLASK, COLOR_WARNING, simulationTg));

        card.getChildren().add(rows);
        return card;
    }

    private VBox buildSignalCard() {
        VBox card = createCard("ПАРАМЕТРИ ДЗВІНКІВ", ICON_WAVEFORM, "#f39c12");
        card.getChildren().add(bellSettingsPane);
        return card;
    }

    private VBox createCard(String title, String icon, String color) {
        VBox card = new VBox(20);
        card.setPadding(new Insets(25));
        card.setStyle(SOFT_CARD);

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(color), 24));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(48, 48);
        iconBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 12;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + "; -fx-letter-spacing: 1.2px;");

        header.getChildren().addAll(iconBox, t);
        card.getChildren().add(header);

        return card;
    }

    private HBox createToggleRow(String title, String desc, String icon, String iconColor, ToggleButton toggle) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setStyle("-fx-background-radius: 12;");

        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(iconColor), 20));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + iconColor + "12; -fx-background-radius: 10;");

        VBox texts = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 13px; -fx-text-fill: #636e72;");
        texts.getChildren().addAll(t, d);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(iconBox, texts, spacer, toggle);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;"));
        return row;
    }

    private void save() {
        try {
            boolean autostartChanged = config.isAutostartEnabled() != autostartTg.isSelected();
            config.setAutostartEnabled(autostartTg.isSelected());
            config.setMinimizeToTray(trayTg.isSelected());
            config.setSimulationMode(simulationTg.isSelected());

            config.setRegularBellDuration(bellSettingsPane.getRegularDuration());
            config.setAirRaidRingDuration(bellSettingsPane.getAirRaidRingDuration());
            config.setAirRaidPauseDuration(bellSettingsPane.getAirRaidPauseDuration());
            config.setEmergencyDuration(bellSettingsPane.getEmergencyDuration());

            mainApp.saveConfig();

            if (autostartChanged) {
                systemService.updateAutostart(config.isAutostartEnabled());
            }

            mainApp.addLog("Системні налаштування оновлено", "SUCCESS");
            ToastService.showSuccess("Налаштування збережено!");
        } catch (Exception e) {
            ToastService.showError("Помилка при збереженні: " + e.getMessage());
        }
    }
}
