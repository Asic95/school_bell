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
        root.setStyle(SOFT_CARD + "-fx-background-radius: 32; -fx-border-radius: 32; -fx-border-width: 2; -fx-border-color: #e2e8f0;");
        root.setPrefWidth(500);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT + ";");
        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #64748b;");

        VBox headerBox = new VBox(4, title, subtitle);

        textField = new TextField(initialValue);
        textField.setPromptText(promptText);
        textField.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-padding: 12 16; -fx-font-size: 15px; -fx-font-weight: 600;");
        
        textField.focusedProperty().addListener((obs, old, newVal) -> {
            if (newVal) {
                textField.setStyle(textField.getStyle() + "-fx-border-color: " + COLOR_PRIMARY + "; -fx-background-color: #f8faff;");
            } else {
                textField.setStyle(textField.getStyle().replace("-fx-border-color: " + COLOR_PRIMARY + ";", "-fx-border-color: #e2e8f0;").replace("-fx-background-color: #f8faff;", "-fx-background-color: #f8fafc;"));
            }
        });

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("СКАСУВАТИ");
        cancelBtn.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #636e72; -fx-font-weight: 800; -fx-padding: 12 24; -fx-background-radius: 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        Button confirmBtn = createPrimaryActionButton("ПІДТВЕРДИТИ", ICON_SAVE);
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
