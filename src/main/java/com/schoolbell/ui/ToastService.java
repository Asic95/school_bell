package com.schoolbell.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
        HBox toast = new HBox(18);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(18, 24, 18, 24));
        toast.setStyle(SOFT_CARD + "-fx-background-radius: 24; -fx-border-radius: 24;");

        // Icon Badge Style
        StackPane iconBox = new StackPane(createSVGIcon(type.icon, Color.WHITE, 20));
        iconBox.setPrefSize(48, 48);
        iconBox.setMinSize(48, 48);
        iconBox.setStyle("-fx-background-color: " + type.gradient + "; -fx-background-radius: 14; -fx-effect: dropshadow(three-pass-box, " + SHADOW_BLACK_10 + ", 10, 0, 0, 2);");

        Label label = new Label(message);
        label.setStyle("-fx-text-fill: " + UIStyles.COLOR_NAVY + "; -fx-font-size: 15px; -fx-font-weight: 900;");
        label.setWrapText(true);
        HBox.setHgrow(label, Priority.ALWAYS);

        toast.getChildren().addAll(iconBox, label);
        toast.setMinWidth(320);
        toast.setMaxWidth(450);

        return toast;
    }

    public static void showSuccess(String message) { show(message, ToastType.SUCCESS); }
    public static void showInfo(String message) { show(message, ToastType.INFO); }
    public static void showError(String message) { show(message, ToastType.ERROR); }
}
