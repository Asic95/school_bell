package com.schoolbell.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static com.schoolbell.ui.UIStyles.*;

/**
 * Premium confirmation dialog for general destructive operations.
 */
public class ConfirmationDialog extends BasePremiumDialog {
    private boolean confirmed = false;

    public ConfirmationDialog(Stage owner, String title, String mainMessage, String detailMessage, String actionText) {
        super(owner,
                "ПІДТВЕРДЖЕННЯ",
                title,
                mainMessage,
                actionText,
                540);

        // Replace standard save button with danger-themed one
        actions.getChildren().remove(saveBtn);
        Button dangerBtn = ControlFactory.createDangerActionButton(actionText, ICON_TRASH);
        dangerBtn.setOnAction(e -> {
            if (onSave()) {
                close();
            }
        });
        actions.getChildren().add(dangerBtn);

        VBox warningBox = new VBox(15);
        warningBox.setPadding(new javafx.geometry.Insets(20));
        warningBox.setStyle("-fx-background-color: " + COLOR_DANGER_PALE + "; -fx-background-radius: 18; -fx-border-color: " + COLOR_DANGER_BORDER + "; -fx-border-width: 1.5; -fx-border-radius: 18;");
        warningBox.setAlignment(Pos.CENTER_LEFT);

        Label warningTitle = new Label("ЦЯ ДІЯ Є НЕЗВОРОТНОЮ");
        warningTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + COLOR_DANGER + ";");
        
        Label warningText = new Label(detailMessage);
        warningText.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_NAVY + "; -fx-line-spacing: 4;");
        warningText.setWrapText(true);

        warningBox.getChildren().addAll(warningTitle, warningText);
        content.getChildren().add(warningBox);
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
