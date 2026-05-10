package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.SchoolClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import static com.schoolbell.ui.UIComponents.createSectionHeader;
import static com.schoolbell.ui.UIStyles.*;

public class ClassesEditorTab {
    private final MainApp mainApp;
    private Runnable refreshClasses;

    public ClassesEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node createContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #f8f9fa;");

        VBox headerArea = createSectionHeader("Шкільні класи та паралелі", "Створюйте та організовуйте класи школи", "#a29bfe", ICON_CLASS);

        TextField addField = new TextField();
        addField.setPromptText("Назва класу (напр. 5-А)...");
        addField.setStyle(COMBO_STYLE);
        addField.setPrefWidth(550);

        Button addBtn = new Button("ДОДАТИ КЛАС");
        addBtn.setStyle("-fx-background-color: #a29bfe; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8;");

        FlowPane listContainer = new FlowPane(15, 15);
        listContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshClasses = () -> {
            listContainer.getChildren().clear();
            for (SchoolClass c : mainApp.getAcademicService().getAllClasses()) {
                VBox card = new VBox(10);
                card.setStyle("-fx-background-color: #f3efff; -fx-background-radius: 16; -fx-padding: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);");
                card.setPrefWidth(120);
                HBox box = new HBox(5);
                box.setAlignment(Pos.CENTER_LEFT);
                TextField edit = new TextField(c.name());
                edit.setPrefWidth(60);
                edit.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 14px;");
                HBox.setHgrow(edit, Priority.ALWAYS);
                edit.focusedProperty().addListener((obs, ov, nv) -> {
                    if (!nv && !edit.getText().equals(c.name()) && !edit.getText().isEmpty()) {
                        mainApp.getAcademicService().updateClass(c.id(), edit.getText());
                        refreshClasses.run();
                    }
                });
                Button del = new Button("✕");
                del.setStyle("-fx-text-fill: #ff7675; -fx-background-color: transparent; -fx-cursor: hand;");
                del.setOnAction(e -> { mainApp.getAcademicService().deleteClass(c.id()); refreshClasses.run(); });
                box.getChildren().addAll(edit, del);
                card.getChildren().add(box);
                listContainer.getChildren().add(card);
            }
        };

        addBtn.setOnAction(e -> {
            if (!addField.getText().isEmpty()) {
                mainApp.getAcademicService().addClass(addField.getText());
                addField.clear();
                refreshClasses.run();
            }
        });
        content.getChildren().addAll(headerArea, new HBox(15, addField, addBtn), scroll);
        refreshClasses.run();
        return content;
    }
}
