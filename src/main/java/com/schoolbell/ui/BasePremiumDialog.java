package com.schoolbell.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import static com.schoolbell.ui.UIStyles.ICON_SAVE;
import static com.schoolbell.ui.UIStyles.MODERN_CHECKBOX_STYLE;
import static com.schoolbell.ui.UIStyles.MODERN_DATE_PICKER_STYLE;
import static com.schoolbell.ui.UIStyles.MODERN_SPINNER_STYLE;

/**
 * Base class for all premium dialogs in the system.
 * Follows the Modern Academic Soft UI v2 design principles.
 */
public abstract class BasePremiumDialog extends Stage {
    protected final VBox root;
    protected final VBox content;
    protected final HBox actions;
    protected final javafx.scene.control.Button saveBtn;
    protected final javafx.scene.control.Button cancelBtn;

    public BasePremiumDialog(Stage owner, String eyebrow, String title, String subtitle, String saveText) {
        this(owner, eyebrow, title, subtitle, saveText, 650);
    }

    public BasePremiumDialog(Stage owner, String eyebrow, String title, String subtitle, String saveText, double width) {
        if (owner != null) {
            initOwner(owner);
        }
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);

        root = ControlFactory.createDialogRoot(width);
        VBox header = ControlFactory.createDialogHeader(eyebrow, title, subtitle);

        content = new VBox(22);
        actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        cancelBtn = ControlFactory.createSecondaryDialogButton("СКАСУВАТИ");
        cancelBtn.setOnAction(e -> close());

        saveBtn = ControlFactory.createPrimaryActionButton(saveText, ICON_SAVE);
        saveBtn.setOnAction(e -> {
            if (onSave()) {
                close();
            }
        });

        actions.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, content, actions);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(
                "data:text/css," + MODERN_DATE_PICKER_STYLE.replace(" ", "%20"),
                "data:text/css," + MODERN_CHECKBOX_STYLE.replace(" ", "%20"),
                "data:text/css," + MODERN_SPINNER_STYLE.replace(" ", "%20")
        );
        setScene(scene);
    }

    /**
     * Adds a custom button to the left of the standard buttons.
     */
    protected void addLeftFooterButton(javafx.scene.control.Button btn) {
        actions.getChildren().add(0, btn);
    }

    /**
     * Subclasses should implement this to validate and save data.
     * @return true if the dialog should be closed after saving.
     */
    protected abstract boolean onSave();

    /**
     * Convenience method to show the dialog and wait for it to close.
     */
    public void display() {
        showAndWait();
    }
}
