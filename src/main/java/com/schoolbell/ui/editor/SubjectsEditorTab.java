package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.Subject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.UIComponents.createSectionHeader;
import static com.schoolbell.ui.UIStyles.*;

public class SubjectsEditorTab {
    private final MainApp mainApp;
    private Runnable refreshSubjects;

    public SubjectsEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node createContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #f8f9fa;");

        VBox headerArea = createSectionHeader("Перелік навчальних дисциплін", "Керуйте списком предметів для навчального процесу", "#00b894", ICON_BOOK);
        TextField addField = new TextField();
        addField.setPromptText("Введіть назву предмета...");
        addField.setStyle(COMBO_STYLE);
        addField.setPrefWidth(550);

        Button addBtn = new Button("ДОДАТИ ПРЕДМЕТ");
        addBtn.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8;");

        FlowPane subjectsContainer = new FlowPane(15, 15);
        subjectsContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(subjectsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshSubjects = () -> {
            subjectsContainer.getChildren().clear();
            for (Subject s : mainApp.getAcademicService().getAllSubjects()) {
                VBox card = new VBox(8);
                card.setStyle("-fx-background-color: #e8f8f5; -fx-background-radius: 16; -fx-padding: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);");
                card.setPrefWidth(240);
                HBox header = new HBox(8);
                header.setAlignment(Pos.CENTER_LEFT);
                TextField edit = new TextField(s.name());
                edit.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 13px;");
                HBox.setHgrow(edit, Priority.ALWAYS);
                edit.focusedProperty().addListener((obs, ov, nv) -> {
                    if (!nv && !edit.getText().equals(s.name()) && !edit.getText().isEmpty()) {
                        mainApp.getAcademicService().updateSubject(s.id(), edit.getText());
                        refreshSubjects.run();
                    }
                });
                Button del = new Button("✕");
                del.setStyle("-fx-text-fill: #ff7675; -fx-background-color: transparent; -fx-cursor: hand;");
                del.setOnAction(e -> { mainApp.getAcademicService().deleteSubject(s.id()); refreshSubjects.run(); });
                header.getChildren().addAll(createSVGIcon(ICON_BOOK, Color.web("#00b894"), 16), edit, del);
                card.getChildren().add(header);
                subjectsContainer.getChildren().add(card);
            }
        };

        addBtn.setOnAction(e -> {
            if (!addField.getText().isEmpty()) {
                mainApp.getAcademicService().addSubject(addField.getText());
                addField.clear();
                refreshSubjects.run();
            }
        });
        content.getChildren().addAll(headerArea, new HBox(15, addField, addBtn), scroll);
        refreshSubjects.run();
        return content;
    }
}
