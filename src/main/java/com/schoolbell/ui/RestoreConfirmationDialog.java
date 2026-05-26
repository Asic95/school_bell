package com.schoolbell.ui;

import javafx.geometry.Pos;
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

        VBox warningBox = new VBox(15);
        warningBox.setPadding(new javafx.geometry.Insets(20));
        warningBox.setStyle("-fx-background-color: " + COLOR_DANGER_PALE + "; -fx-background-radius: 18; -fx-border-color: " + COLOR_DANGER_BORDER + "; -fx-border-radius: 18;");
        warningBox.setAlignment(Pos.CENTER_LEFT);

        Label warningTitle = new Label("ЦЯ ДІЯ Є НЕЗВОРОТНЬОЮ");
        warningTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + COLOR_DANGER + ";");
        
        Label warningText = new Label("Всі зміни, внесені після створення обраної резервної копії, будуть втрачені. " +
                "Система повністю перезапише поточну базу даних.");
        warningText.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_NAVY + ";");
        warningText.setWrapText(true);

        warningBox.getChildren().addAll(warningTitle, warningText);
        
        content.getChildren().add(warningBox);
        
        // Update save button style to reflect danger and handle hover correctly
        String redGradient = "linear-gradient(to right, " + COLOR_INDIGO + ", " + COLOR_DANGER + ")";
        saveBtn.getProperties().put("baseGradient", redGradient);
        ControlFactory.applyActionButtonStyle(saveBtn, false);
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
