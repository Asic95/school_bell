package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.BroadcastDevice;
import com.schoolbell.service.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class EfirSideSection extends VBox {
    private final MainApp mainApp;
    private final VBox deviceListContainer;

    public EfirSideSection(MainApp mainApp) {
        super(28);
        this.mainApp = mainApp;
        this.deviceListContainer = new VBox(12);

        // Devices Card
        VBox devicesCard = new VBox(20);
        devicesCard.setPadding(new Insets(28));
        devicesCard.setStyle(SOFT_CARD);

        HBox devicesHeader = new HBox(15);
        devicesHeader.setAlignment(Pos.CENTER_LEFT);
        Label dTitle = new Label("ПРИСТРОЇ ТА МОНІТОРИ");
        dTitle.setStyle(HEADER_STYLE);
        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
        
        Button refreshBtn = new Button("ОНОВИТИ");
        refreshBtn.setGraphic(createSVGIcon(ICON_REFRESH, Color.web(COLOR_PRIMARY), 16));
        refreshBtn.setGraphicTextGap(10);
        String refreshBtnBase = "-fx-background-color: white; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 8 16; -fx-background-radius: 12; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 12; -fx-cursor: hand;";
        refreshBtn.setStyle(refreshBtnBase);
        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle(refreshBtnBase + "-fx-background-color: " + COLOR_SURFACE_GLASS_START + "; -fx-border-color: " + COLOR_PRIMARY + ";"));
        refreshBtn.setOnMouseExited(e -> refreshBtn.setStyle(refreshBtnBase));
        refreshBtn.setOnAction(e -> refreshDevices());
        devicesHeader.getChildren().addAll(dTitle, s, refreshBtn);

        deviceListContainer.setPadding(new Insets(5, 0, 0, 0));
        devicesCard.getChildren().addAll(devicesHeader, deviceListContainer);

        getChildren().add(devicesCard);
        
        refreshDevices();
    }

    public void refreshDevices() {
        List<String> activeIps = mainApp.getBroadcastService() != null ? mainApp.getBroadcastService().getConnectedClients() : List.of();
        List<BroadcastDevice> savedDevices = DatabaseManager.getAllBroadcastDevices();

        updateContainer(deviceListContainer, savedDevices, device -> new DeviceRow(device, activeIps.contains(device.ip()), 
            () -> {
                // Edit logic
                new TextInputModalDialog(mainApp.getStage(), "Редагування пристрою", "Введіть нову назву для " + device.ip(), device.name(), "Назва пристрою", newName -> {
                    BroadcastDevice updated = new BroadcastDevice(device.ip(), newName, device.isBanned(), device.deviceType(), device.os(), device.lastSeen());
                    DatabaseManager.saveBroadcastDevice(updated);
                    refreshDevices();
                }).display();
            },
            () -> {
                // Ban/Unban logic
                boolean newBannedState = !device.isBanned();
                BroadcastDevice toggled = new BroadcastDevice(device.ip(), device.name(), newBannedState, device.deviceType(), device.os(), device.lastSeen());
                DatabaseManager.saveBroadcastDevice(toggled);
                if (mainApp.getBroadcastService() != null) {
                    mainApp.getBroadcastService().loadBannedIps();
                    if (newBannedState) {
                        mainApp.getBroadcastService().getConnections().stream()
                                .filter(c -> c.getRemoteSocketAddress() != null && c.getRemoteSocketAddress().getAddress().getHostAddress().equals(device.ip()))
                                .forEach(c -> c.close(4003, "IP is banned"));
                    }
                }
                refreshDevices();
            },
            () -> {
                // Delete logic
                DatabaseManager.deleteBroadcastDevice(device.ip());
                refreshDevices();
            }
        ));
    }

    private <T> void updateContainer(Pane container, List<T> items, java.util.function.Function<T, Node> mapper) {
        container.getChildren().setAll(items.stream().map(mapper).toList());
    }
}
