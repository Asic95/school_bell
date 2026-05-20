package com.schoolbell.ui;

import com.schoolbell.model.BroadcastDevice;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class DeviceRow extends HBox {
    public DeviceRow(BroadcastDevice device, boolean isActive, Runnable onEdit, Runnable onBan, Runnable onDelete) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setPadding(new Insets(12, 0, 12, 0));

        VBox iconBox = new VBox(createSVGIcon(ICON_MONITOR, Color.web(isActive ? COLOR_PRIMARY : COLOR_ZINC_500), 22));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(48, 48);
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.setStyle("-fx-background-color: " + (isActive ? COLOR_PRIMARY + "10" : "#f1f2f6") + "; -fx-background-radius: 16;");

        VBox info = new VBox(3);
        Label name = new Label(device.name());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_TEXT + ";");
        
        Label ip = new Label(device.ip());
        ip.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + COLOR_ZINC_500 + ";");
        
        HBox statusLine = new HBox(6);
        statusLine.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(3.5, Color.web(isActive ? COLOR_SUCCESS : COLOR_DANGER));
        Label statusTxt = new Label(isActive ? "В МЕРЕЖІ" : "ОФЛАЙН");
        statusTxt.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: " + (isActive ? COLOR_SUCCESS : COLOR_DANGER) + ";");
        statusLine.getChildren().addAll(dot, statusTxt);
        
        info.getChildren().addAll(name, ip, statusLine);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getChildren().addAll(
            createCardActionButton(ICON_EDIT, "#f1f2f6", COLOR_PRIMARY, onEdit),
            createCardActionButton(ICON_BAN, "#f1f2f6", COLOR_NEUTRAL, onBan),
            createCardActionButton(ICON_TRASH, "#fff5f5", COLOR_DANGER, onDelete)
        );

        getChildren().addAll(iconBox, info, actions);
    }
}
