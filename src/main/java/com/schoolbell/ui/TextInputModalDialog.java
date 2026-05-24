package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

import static com.schoolbell.ui.ControlFactory.createDialogHeader;
import static com.schoolbell.ui.ControlFactory.createDialogRoot;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createSecondaryDialogButton;
import static com.schoolbell.ui.UIStyles.ICON_SAVE;
import static com.schoolbell.ui.UIStyles.PREMIUM_BTN_STYLE;
import static com.schoolbell.ui.UIStyles.PREMIUM_FIELD_FOCUSED_STYLE;
import static com.schoolbell.ui.UIStyles.PREMIUM_FIELD_STYLE;

public class TextInputModalDialog extends Stage {
    private final TextField textField;
    private final Consumer<String> onResult;

    public TextInputModalDialog(MainApp mainApp, String titleText, String subtitleText, String initialValue, String promptText, Consumer<String> onResult) {
        this.onResult = onResult;

        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        initOwner(mainApp.getStage());

        VBox root = createDialogRoot(500);
        VBox headerBox = createDialogHeader("Дія", titleText, subtitleText);

        textField = new TextField(initialValue);
        textField.setPromptText(promptText);
        textField.setStyle(PREMIUM_FIELD_STYLE);
        textField.focusedProperty().addListener((obs, old, focused) ->
                textField.setStyle(focused ? PREMIUM_FIELD_FOCUSED_STYLE : PREMIUM_FIELD_STYLE));

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = createSecondaryDialogButton("СКАСУВАТИ");
        cancelBtn.setOnAction(e -> close());

        Button confirmBtn = createPrimaryActionButton("ПІДТВЕРДИТИ", ICON_SAVE);
        confirmBtn.setStyle(PREMIUM_BTN_STYLE);
        confirmBtn.setOnAction(ev -> {
            String val = textField.getText().trim();
            if (!val.isEmpty()) {
                onResult.accept(val);
                close();
            } else {
                ToastService.showError("Поле не може бути порожнім");
            }
        });

        actions.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(headerBox, textField, actions);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
    }
}
