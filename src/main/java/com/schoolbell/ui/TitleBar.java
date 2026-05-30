package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.InputStream;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class TitleBar extends HBox {
    private final Stage stage;
    private final MainApp app;
    private double xOffset = 0;
    private double yOffset = 0;
    private double prevX, prevY, prevWidth, prevHeight;
    private boolean isMaximizedManual = true;
    private final Button maxBtn;

    public TitleBar(Stage stage, MainApp app, String appTitle) {
        this.stage = stage;
        this.app = app;

        setSpacing(15);
        setStyle(TITLE_BAR_STYLE);
        setPrefHeight(45);
        setMinHeight(45);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 15, 0, 15));

        // Dragging support
        setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        setOnMouseDragged(e -> {
            if (!isMaximizedManual) {
                stage.setX(e.getScreenX() - xOffset);
                stage.setY(e.getScreenY() - yOffset);
            }
        });
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                toggleMaximize();
            }
        });

        // Left section
        HBox leftBox = new HBox(12);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        try {
            InputStream is = getClass().getResourceAsStream("/icon.png");
            if (is != null) {
                ImageView iv = new ImageView(new Image(is));
                iv.setFitHeight(22);
                iv.setFitWidth(22);
                iv.setPreserveRatio(true);
                leftBox.getChildren().add(iv);
            }
        } catch (Exception e) {
            leftBox.getChildren().add(createSVGIcon(ICON_BELL, Color.WHITE, 18));
        }

        Label title = new Label(appTitle);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 800; -fx-letter-spacing: 0.8px;");
        leftBox.getChildren().add(title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right section (Buttons)
        Button minBtn = createTitleBarButton(ICON_WINDOW_MINIMIZE, "white", e -> stage.setIconified(true));
        
        maxBtn = createTitleBarButton(isMaximizedManual ? ICON_WINDOW_RESTORE : ICON_WINDOW_MAXIMIZE, "white", null);
        maxBtn.setOnAction(e -> toggleMaximize());

        Button closeBtn = createTitleBarButton(ICON_WINDOW_CLOSE, "white", e -> {
            if (app.getConfigService().isMinimizeToTray()) {
                stage.hide();
            } else {
                app.stop();
                Platform.exit();
                System.exit(0);
            }
        });
        closeBtn.setOnMouseEntered(e -> {
            closeBtn.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 10;");
            closeBtn.setGraphic(createSVGIcon(ICON_WINDOW_CLOSE, Color.WHITE, 16));
        });
        closeBtn.setOnMouseExited(e -> {
            closeBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10;");
            closeBtn.setGraphic(createSVGIcon(ICON_WINDOW_CLOSE, Color.WHITE, 16));
        });

        getChildren().addAll(leftBox, spacer, minBtn, maxBtn, closeBtn);
    }

    private void toggleMaximize() {
        javafx.geometry.Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        if (!isMaximizedManual) {
            prevX = stage.getX();
            prevY = stage.getY();
            prevWidth = stage.getWidth();
            prevHeight = stage.getHeight();
            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());
            isMaximizedManual = true;
        } else {
            if (prevWidth == 0) {
                stage.setWidth(1400);
                stage.setHeight(950);
                stage.centerOnScreen();
            } else {
                stage.setX(prevX);
                stage.setY(prevY);
                stage.setWidth(prevWidth);
                stage.setHeight(prevHeight);
            }
            isMaximizedManual = false;
        }
        maxBtn.setGraphic(createSVGIcon(isMaximizedManual ? ICON_WINDOW_RESTORE : ICON_WINDOW_MAXIMIZE, Color.WHITE, 16));
    }

    private Button createTitleBarButton(String icon, String color, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button();
        btn.setGraphic(createSVGIcon(icon, Color.web(color), 16));
        btn.setPrefSize(38, 38);
        btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;");
        btn.setOnAction(action);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 10; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;"));
        return btn;
    }
}
