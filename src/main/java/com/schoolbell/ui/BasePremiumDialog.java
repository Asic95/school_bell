package com.schoolbell.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import static com.schoolbell.ui.UIStyles.ICON_SAVE;
import static com.schoolbell.ui.UIStyles.MODERN_CHECKBOX_STYLE;
import static com.schoolbell.ui.UIStyles.MODERN_DATE_PICKER_STYLE;
import static com.schoolbell.ui.UIStyles.MODERN_SPINNER_STYLE;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

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

        // Wrap root in a transparent StackPane with padding to prevent shadow clipping
        // and eliminate grey corner artifacts caused by non-transparent window bounds.
        StackPane wrapper = new StackPane(root);
        wrapper.setStyle("-fx-background-color: transparent;");
        wrapper.setPadding(new Insets(40)); // Space for the 30px shadow

        Scene scene = new Scene(wrapper);
        scene.setFill(Color.TRANSPARENT);
        
        // Use Base64 encoding for data: URIs to safely handle CSS special characters like '#'
        scene.getStylesheets().addAll(
                encodeCssToDataUri(MODERN_DATE_PICKER_STYLE),
                encodeCssToDataUri(MODERN_CHECKBOX_STYLE),
                encodeCssToDataUri(MODERN_SPINNER_STYLE)
        );
        setScene(scene);
    }

    private String encodeCssToDataUri(String css) {
        String base64 = Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        return "data:text/css;base64," + base64;
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
