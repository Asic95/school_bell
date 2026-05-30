package com.schoolbell.ui;

import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Consumer;

import static com.schoolbell.ui.UIStyles.PREMIUM_FIELD_FOCUSED_STYLE;
import static com.schoolbell.ui.UIStyles.PREMIUM_FIELD_STYLE;

/**
 * Premium text input dialog following the Modern Academic Soft UI v2 design.
 */
public class TextInputModalDialog extends BasePremiumDialog {
    private final TextField textField;
    private final Consumer<String> onResult;

    public TextInputModalDialog(Stage owner, String titleText, String subtitleText, String initialValue, String promptText, Consumer<String> onResult) {
        super(owner, "ДІЯ", titleText, subtitleText, "ПІДТВЕРДИТИ", 540);
        this.onResult = onResult;

        textField = new TextField(initialValue);
        textField.setPromptText(promptText);
        textField.setStyle(PREMIUM_FIELD_STYLE);
        textField.focusedProperty().addListener((obs, old, focused) ->
                textField.setStyle(focused ? PREMIUM_FIELD_FOCUSED_STYLE : PREMIUM_FIELD_STYLE));

        content.getChildren().add(textField);
    }

    @Override
    protected boolean onSave() {
        String val = textField.getText().trim();
        if (!val.isEmpty()) {
            onResult.accept(val);
            return true;
        } else {
            ToastService.showError("Поле не може бути порожнім");
            return false;
        }
    }
}
