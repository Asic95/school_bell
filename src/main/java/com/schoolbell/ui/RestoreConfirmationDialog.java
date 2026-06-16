package com.schoolbell.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static com.schoolbell.ui.UIStyles.*;

/**
 * Premium confirmation dialog for destructive database operations.
 */
public class RestoreConfirmationDialog extends BasePremiumDialog {
    private boolean confirmed = false;

    public RestoreConfirmationDialog(Stage owner) {
        super(owner,
                "БЕЗПЕКА ДАНИХ",
                "Підтвердження відновлення",
                "УВАГА: Поточні налаштування та розклади будуть повністю замінені.",
                "ПІДТВЕРДИТИ ВІДНОВЛЕННЯ",
                580);

        // Replace standard save button with danger-themed one
        actions.getChildren().remove(saveBtn);
        Button dangerBtn = ControlFactory.createDangerActionButton("ПІДТВЕРДИТИ ВІДНОВЛЕННЯ", ICON_REFRESH);
        dangerBtn.setOnAction(e -> {
            if (onSave()) {
                close();
            }
        });
        actions.getChildren().add(dangerBtn);

        VBox warningBox = new VBox(15);
        // ... (rest of warningBox setup)
        warningBox.setPadding(new javafx.geometry.Insets(20));
        warningBox.setStyle("-fx-background-color: " + COLOR_DANGER_PALE + "; -fx-background-radius: 18; -fx-border-color: " + COLOR_DANGER_BORDER + "; -fx-border-width: 1.5; -fx-border-radius: 18;");
        warningBox.setAlignment(Pos.CENTER_LEFT);

        Label warningTitle = new Label("ЦЯ ДІЯ Є НЕЗВОРОТНОЮ");
        warningTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + COLOR_DANGER + ";");
        
        Label warningText = new Label("Всі зміни, внесені після створення обраної резервної копії, будуть втрачені. " +
                "Система повністю перезапише поточну базу даних.");
        warningText.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_NAVY + "; -fx-line-spacing: 4;");
        warningText.setWrapText(true);

        warningBox.getChildren().addAll(warningTitle, warningText);
        
        content.getChildren().add(warningBox);
        
        setupCountdown(dangerBtn, 7);
    }

    private void setupCountdown(Button btn, int seconds) {
        btn.setDisable(true);
        final String baseText = btn.getText();
        
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        for (int i = 0; i <= seconds; i++) {
            final int remaining = seconds - i;
            javafx.animation.KeyFrame frame = new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(i),
                e -> {
                    if (remaining > 0) {
                        btn.setText(baseText + " (" + remaining + ")");
                    } else {
                        btn.setText(baseText);
                        btn.setDisable(false);
                    }
                }
            );
            timeline.getKeyFrames().add(frame);
        }
        timeline.play();
    }

    @Override
    protected boolean onSave() {
        confirmed = true;
        return true;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
