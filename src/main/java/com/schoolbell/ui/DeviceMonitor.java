package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.BroadcastDevice;
import com.schoolbell.service.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;

import static com.schoolbell.ui.ControlFactory.createSettingsSection;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class DeviceMonitor {
    private final MainApp mainApp;
    private final VBox deviceListContainer;

    public DeviceMonitor(MainApp mainApp) {
        this.mainApp = mainApp;
        this.deviceListContainer = new VBox(10);
    }

    public Node build() {
        VBox deviceCard = createSettingsSection("КЕРУВАННЯ ПРИСТРОЯМИ", COLOR_PURPLE_DARK, ICON_MONITOR);
        deviceCard.setStyle(SOFT_CARD);
        deviceCard.setPadding(new Insets(25));
        
        Button refreshBtn = new Button("ОНОВИТИ СПИСОК");
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + COLOR_BLUE + "; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> refreshDevices());

        HBox header = (HBox) deviceCard.getChildren().get(0);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(spacer, refreshBtn);

        deviceCard.getChildren().add(deviceListContainer);
        refreshDevices();

        return deviceCard;
    }

    public void refreshDevices() {
        deviceListContainer.getChildren().clear();
        
        HBox listHeader = new HBox(20);
        listHeader.setPadding(new Insets(10, 20, 10, 20));
        listHeader.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + "; -fx-background-radius: 12;");
        
        Label hDevice = new Label("ПРИСТРІЙ"); hDevice.setPrefWidth(300);
        Label hNetwork = new Label("МЕРЕЖА"); hNetwork.setPrefWidth(180);
        Label hLastSeen = new Label("ОСТАННЯ АКТИВНІСТЬ"); hLastSeen.setPrefWidth(200);
        Label hActions = new Label("ДІЇ");
        
        String hStyle = "-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 1px;";
        hDevice.setStyle(hStyle); hNetwork.setStyle(hStyle); hLastSeen.setStyle(hStyle); hActions.setStyle(hStyle);
        
        listHeader.getChildren().addAll(hDevice, hNetwork, hLastSeen, hActions);
        deviceListContainer.getChildren().add(listHeader);

        List<String> activeIps = mainApp.getBroadcastService() != null ? mainApp.getBroadcastService().getConnectedClients() : List.of();
        List<BroadcastDevice> savedDevices = DatabaseManager.getAllBroadcastDevices();
        
        if (savedDevices.isEmpty() && activeIps.isEmpty()) {
            Label none = new Label("Немає підключених пристроїв");
            none.setStyle("-fx-font-style: italic; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-padding: 30;");
            deviceListContainer.getChildren().add(none);
        } else {
            for (BroadcastDevice device : savedDevices) {
                deviceListContainer.getChildren().add(createModernDeviceRow(device, activeIps.contains(device.ip())));
            }
        }
    }

    private Node createModernDeviceRow(BroadcastDevice device, boolean isActive) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));
        row.setStyle("-fx-background-color: white; -fx-border-color: " + COLOR_SURFACE_SUBTLE + "; -fx-border-width: 0 0 1 0;");
        
        // --- DEVICE INFO ---
        VBox deviceBox = new VBox(4);
        deviceBox.setPrefWidth(300);
        
        String iconPath = ICON_MONITOR;
        if ("MOBILE".equals(device.deviceType())) iconPath = ICON_PHONE;
        else if ("TABLET".equals(device.deviceType())) iconPath = ICON_TABLET;
        
        HBox nameLine = new HBox(12);
        nameLine.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconCircle = new VBox(createSVGIcon(iconPath, Color.web(isActive ? COLOR_PRIMARY : COLOR_TEXT_DIM), 20));
        iconCircle.setAlignment(Pos.CENTER);
        iconCircle.setPrefSize(40, 40);
        iconCircle.setStyle("-fx-background-color: " + (isActive ? COLOR_BLUE_LIGHT : COLOR_SURFACE_SUBTLE) + "; -fx-background-radius: 10;");
        
        VBox labels = new VBox(2);
        Label name = new Label(device.name());
        name.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label os = new Label(device.os() + " • " + device.deviceType());
        os.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        labels.getChildren().addAll(name, os);
        
        nameLine.getChildren().addAll(iconCircle, labels);
        deviceBox.getChildren().add(nameLine);

        // --- NETWORK ---
        VBox netBox = new VBox(4);
        netBox.setPrefWidth(180);
        Label ip = new Label(device.ip());
        ip.setStyle("-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        HBox statusLine = new HBox(6);
        statusLine.setAlignment(Pos.CENTER_LEFT);
        
        String statusColor = device.isBanned() ? COLOR_TEXT_DIM : (isActive ? COLOR_SUCCESS : COLOR_DANGER);
        String statusTxt = device.isBanned() ? "ЗАБЛОКОВАНО" : (isActive ? "В мережі" : "Поза мережею");
        
        Circle dot = new Circle(4, Color.web(statusColor));
        Label status = new Label(statusTxt);
        status.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");
        statusLine.getChildren().addAll(dot, status);
        netBox.getChildren().addAll(ip, statusLine);

        // --- LAST SEEN ---
        Label lastSeenText = new Label(device.lastSeen());
        lastSeenText.setPrefWidth(200);
        lastSeenText.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-weight: bold;");

        // --- ACTIONS ---
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button editBtn = createIconButton(ICON_EDIT, COLOR_PRIMARY, () -> editDeviceName(device));
        Button banBtn = new Button(device.isBanned() ? "ДОЗВОЛИТИ" : "БЛОКУВАТИ");
        banBtn.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-background-color: " + (device.isBanned() ? COLOR_SUCCESS : COLOR_DANGER) + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 5 10; -fx-cursor: hand;");
        banBtn.setOnAction(e -> toggleBan(device));
        
        Button delBtn = createIconButton(ICON_TRASH, COLOR_DANGER, () -> deleteDevice(device));
        
        actions.getChildren().addAll(editBtn, banBtn, delBtn);
        
        row.getChildren().addAll(deviceBox, netBox, lastSeenText, actions);
        if (device.isBanned()) row.setOpacity(0.7);
        
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + COLOR_SURFACE_FAINT + "; -fx-border-color: " + COLOR_SURFACE_SUBTLE + "; -fx-border-width: 0 0 1 0; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: white; -fx-border-color: " + COLOR_SURFACE_SUBTLE + "; -fx-border-width: 0 0 1 0;"));
        
        return row;
    }

    private Button createIconButton(String icon, String color, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(createSVGIcon(icon, Color.web(color), 16));
        btn.setStyle("-fx-background-color: " + color + "10; -fx-background-radius: 8; -fx-padding: 8; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + color + "25; -fx-background-radius: 8; -fx-padding: 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "10; -fx-background-radius: 8; -fx-padding: 8;"));
        return btn;
    }

    private void editDeviceName(BroadcastDevice device) {
        TextInputDialog dialog = new TextInputDialog(device.name());
        dialog.setTitle("Налаштування пристрою");
        dialog.setHeaderText("Змінити назву для " + device.ip());
        dialog.setContentText("Назва:");
        dialog.showAndWait().ifPresent(newName -> {
            DatabaseManager.saveBroadcastDevice(new BroadcastDevice(device.ip(), newName, device.isBanned(), device.deviceType(), device.os(), device.lastSeen()));
            refreshDevices();
        });
    }

    private void toggleBan(BroadcastDevice device) {
        DatabaseManager.saveBroadcastDevice(new BroadcastDevice(device.ip(), device.name(), !device.isBanned(), device.deviceType(), device.os(), device.lastSeen()));
        if (mainApp.getBroadcastService() != null) {
            mainApp.getBroadcastService().loadBannedIps();
            if (!device.isBanned()) {
                mainApp.getBroadcastService().getConnections().stream()
                        .filter(c -> c.getRemoteSocketAddress().getAddress().getHostAddress().equals(device.ip()))
                        .forEach(c -> c.close(4003, "IP is banned"));
            }
        }
        refreshDevices();
    }

    private void deleteDevice(BroadcastDevice device) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Видалити історію для " + device.ip() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                DatabaseManager.deleteBroadcastDevice(device.ip());
                refreshDevices();
            }
        });
    }
}
