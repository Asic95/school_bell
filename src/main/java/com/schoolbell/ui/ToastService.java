package com.schoolbell.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class ToastService {
    private static VBox toastContainer;

    public enum ToastType {
        SUCCESS(COLOR_SUCCESS, ICON_CHECK, GRADIENT_SUCCESS),
        INFO(COLOR_PRIMARY, ICON_INFO, GRADIENT_INFO),
        ERROR(COLOR_DANGER, ICON_DANGER, GRADIENT_DANGER);

        final String color;
        final String icon;
        final String gradient;

        ToastType(String color, String icon, String gradient) {
            this.color = color;
            this.icon = icon;
            this.gradient = gradient;
        }
    }

    public static void setup(StackPane root) {
        toastContainer = new VBox(10);
        toastContainer.setPadding(new Insets(20));
        toastContainer.setAlignment(Pos.BOTTOM_RIGHT);
        toastContainer.setPickOnBounds(false); // Allow clicking through the container
        toastContainer.setMaxWidth(400);
        StackPane.setAlignment(toastContainer, Pos.BOTTOM_RIGHT);
        root.getChildren().add(toastContainer);
    }

    public static void show(String message, ToastType type) {
        if (toastContainer == null) {
            System.err.println("ToastService not initialized. Call setup(root) first.");
            return;
        }

        Platform.runLater(() -> {
            HBox toast = createToastNode(message, type);
            toastContainer.getChildren().add(toast);

            // Animations
            toast.setOpacity(0);
            toast.setTranslateX(100);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
            fadeIn.setToValue(1);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toast);
            slideIn.setToX(0);

            PauseTransition delay = new PauseTransition(Duration.seconds(8));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toast);
            fadeOut.setToValue(0);

            SequentialTransition sequence = new SequentialTransition(
                    new javafx.animation.ParallelTransition(fadeIn, slideIn),
                    delay,
                    fadeOut
            );

            sequence.setOnFinished(e -> toastContainer.getChildren().remove(toast));
            sequence.play();
        });
    }

    private static HBox createToastNode(String message, ToastType type) {
        HBox toast = new HBox(15);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(15, 20, 15, 20));
        toast.setStyle("-fx-background-color: white; " +
                       "-fx-background-radius: 20; " +
                       "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 20, 0, 0, 10); " +
                       "-fx-border-color: #dfe6e9; " +
                       "-fx-border-radius: 20; " +
                       "-fx-border-width: 1;");

        // Icon circle
        StackPane iconBox = new StackPane(createSVGIcon(type.icon, Color.WHITE, 18));
        iconBox.setPrefSize(40, 40);
        iconBox.setMinSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + type.gradient + "; -fx-background-radius: 12;");

        Label label = new Label(message);
        label.setStyle("-fx-text-fill: " + COLOR_TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        label.setWrapText(true);
        HBox.setHgrow(label, Priority.ALWAYS);

        toast.getChildren().addAll(iconBox, label);
        toast.setMinWidth(300);
        toast.setMaxWidth(380);

        return toast;
    }

    public static void showSuccess(String message) { show(message, ToastType.SUCCESS); }
    public static void showInfo(String message) { show(message, ToastType.INFO); }
    public static void showError(String message) { show(message, ToastType.ERROR); }
}
