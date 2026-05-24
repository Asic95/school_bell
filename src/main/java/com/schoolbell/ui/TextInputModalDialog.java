package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.UIStyles.*;

public class TextInputModalDialog extends Stage {
    private final TextField textField;
    private final Consumer<String> onResult;

    public TextInputModalDialog(MainApp mainApp, String titleText, String subtitleText, String initialValue, String promptText, Consumer<String> onResult) {
        this.onResult = onResult;

        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        initOwner(mainApp.getStage());

        VBox root = new VBox(25);
        root.setPadding(new Insets(35));
        root.setStyle(SOFT_CARD);
        root.setPrefWidth(500);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 500; -fx-text-fill: #64748b;");

        VBox headerBox = new VBox(8, title, subtitle);

        textField = new TextField(initialValue);
        textField.setPromptText(promptText);
        textField.setStyle(PREMIUM_FIELD_STYLE);
        
        textField.focusedProperty().addListener((obs, old, newVal) -> {
            if (newVal) {
                textField.setStyle(PREMIUM_FIELD_STYLE + "-fx-border-color: #4f46e5; -fx-background-color: #f8faff;");
            } else {
                textField.setStyle(PREMIUM_FIELD_STYLE);
            }
        });

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("СКАСУВАТИ");
        String cancelStyle = "-fx-background-color: white; -fx-text-fill: #64748b; -fx-font-weight: 800; -fx-padding: 12 24; -fx-background-radius: 18; -fx-border-color: #e2e8f0; -fx-border-radius: 18; -fx-cursor: hand;";
        cancelBtn.setStyle(cancelStyle);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelStyle + "-fx-background-color: #f1f2f6;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(cancelStyle));
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
