package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.UIStyles.*;
import static com.schoolbell.ui.UIComponents.createSVGIcon;

public class EfirNetworkSection extends VBox {
    private final MainApp mainApp;
    private final ConfigService config;

    private final TextField schoolNameField;
    private final TextField cityNameField;
    private final TextField portField;
    private final ComboBox<String> themeCombo;
    private final Label firewallStatusLabel;
    private Button optimizeBtn;

    public EfirNetworkSection(MainApp mainApp, EfirStatusCard statusCard) {
        super(22);
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();

        this.schoolNameField = createStyledField(config.getSchoolName());
        this.cityNameField = createStyledField(config.getCityName());
        this.portField = createStyledField(String.valueOf(config.getBroadcastPort()));
        
        this.themeCombo = new ComboBox<>();
        this.themeCombo.getItems().addAll("classic", "modern", "modern_full", "panorama", "neo", "cyber", "soft");
        this.themeCombo.setValue(config.getDashboardTheme());
        this.themeCombo.setStyle(PREMIUM_SELECT_STYLE);
        this.themeCombo.setPrefWidth(200);

        this.firewallStatusLabel = new Label("ПЕРЕВІРКА...");
        this.firewallStatusLabel.setMaxWidth(Double.MAX_VALUE);
        
        schoolNameField.setPromptText("Назва закладу");
        cityNameField.setPromptText("Місто");
        portField.setPrefWidth(120);

        setupAutoSaveListeners(statusCard);

        Label title = new Label("МЕРЕЖА ТА ТРАНСЛЯЦІЯ");
        title.setStyle(HEADER_STYLE);

        HBox mainRow = new HBox(24);
        mainRow.setAlignment(Pos.TOP_LEFT);

        this.optimizeBtn = createPrimaryActionButton("ОПТИМІЗУВАТИ", ICON_UPDATE);
        this.optimizeBtn.setMaxWidth(Double.MAX_VALUE);
        this.optimizeBtn.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                mainApp.getSystemService().optimizeFirewall(port);
                updateFirewallStatusLabel();
            } catch (NumberFormatException ex) {
                ToastService.showError("Некоректний порт");
            }
        });

        mainRow.getChildren().addAll(
            createModernSettingsGroup("ЗАКЛАД", ICON_SCHOOL, COLOR_INDIGO, new VBox(12, createLabeledField("НАЗВА", schoolNameField), createLabeledField("МІСТО", cityNameField))),
            createModernSettingsGroup("ТРАНСЛЯЦІЯ", ICON_NET, COLOR_VIOLET, new VBox(12, createLabeledField("ПОРТ ТРАНСЛЯЦІЇ", portField))),
            createModernSettingsGroup("ОФОРМЛЕННЯ", ICON_AIRPLAY, COLOR_PRIMARY, new VBox(12, createLabeledField("ДИЗАЙН ТАБЛО", themeCombo))),
            createModernSettingsGroup("БРАНДМАУЕР", ICON_SHIELD, COLOR_TEAL_DARK, new VBox(15, firewallStatusLabel, optimizeBtn))
        );
        
        mainRow.getChildren().forEach(n -> ((VBox)n).setMinWidth(320));

        getChildren().addAll(title, mainRow);
        
        updateFirewallStatusLabel();
    }

    private void setupAutoSaveListeners(EfirStatusCard statusCard) {
        schoolNameField.focusedProperty().addListener((obs, ov, nv) -> { if (!nv) save(statusCard); });
        cityNameField.focusedProperty().addListener((obs, ov, nv) -> { if (!nv) save(statusCard); });
        portField.focusedProperty().addListener((obs, ov, nv) -> { if (!nv) save(statusCard); });
        themeCombo.setOnAction(e -> save(statusCard));
    }

    private void save(EfirStatusCard statusCard) {
        try {
            config.setSchoolName(schoolNameField.getText());
            config.setCityName(cityNameField.getText());
            config.setBroadcastPort(Integer.parseInt(portField.getText()));
            config.setDashboardTheme(themeCombo.getValue());
            
            mainApp.saveConfig();
            if (config.isBroadcastEnabled()) {
                mainApp.startBroadcastServers();
            } else {
                mainApp.stopBroadcastServers();
            }
            updateFirewallStatusLabel();
            if (statusCard != null) {
                statusCard.refreshAddress();
            }
            ToastService.showSuccess("Налаштування ефіру оновлено");
        } catch (Exception ex) {
            // Silently ignore invalid port numbers etc
        }
    }

    public void updateFirewallStatusLabel() {
        javafx.application.Platform.runLater(() -> {
            updateStatusBadge("ПЕРЕВІРКА...", ICON_REFRESH, COLOR_SLATE, COLOR_SURFACE_SOFT, COLOR_BORDER_SOFT);
            if (optimizeBtn != null) optimizeBtn.setDisable(true);
        });

        new Thread(() -> {
            try {
                int port = Integer.parseInt(portField.getText());
                boolean allowed = mainApp.getSystemService().isPortAllowedInFirewall(port);
                javafx.application.Platform.runLater(() -> {
                    if (allowed) {
                        updateStatusBadge("ВІДКРИТО", ICON_CHECK, COLOR_SUCCESS, COLOR_SUCCESS_PALE, COLOR_SUCCESS_BORDER);
                    } else {
                        updateStatusBadge("ЗАБЛОКОВАНО", ICON_BAN, COLOR_DANGER, COLOR_DANGER_SOFT, COLOR_DANGER_BORDER);
                    }
                    if (optimizeBtn != null) optimizeBtn.setDisable(allowed);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    updateStatusBadge("НЕВІДОМО", ICON_INFO, COLOR_SLATE, COLOR_SURFACE_SOFT, COLOR_BORDER_SOFT);
                    if (optimizeBtn != null) optimizeBtn.setDisable(false);
                });
            }
        }).start();
    }

    private void updateStatusBadge(String text, String iconPath, String textColor, String bgHex, String borderHex) {
        firewallStatusLabel.setText(text);
        firewallStatusLabel.setGraphic(createSVGIcon(iconPath, javafx.scene.paint.Color.web(textColor), 14));
        firewallStatusLabel.setGraphicTextGap(8);
        firewallStatusLabel.setStyle(
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 800; " +
            "-fx-font-family: 'Inter'; " +
            "-fx-text-fill: " + textColor + "; " +
            "-fx-background-color: " + bgHex + "; " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: " + borderHex + "; " +
            "-fx-border-radius: 12px; " +
            "-fx-border-width: 1.5px; " +
            "-fx-padding: 8 16 8 16; " +
            "-fx-alignment: CENTER;"
        );
    }
}
