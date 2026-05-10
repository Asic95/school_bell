package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.Classroom;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.UIComponents.createSectionHeader;
import static com.schoolbell.ui.UIStyles.*;

public class ClassroomsEditorTab {
    private final MainApp mainApp;
    private Runnable refreshClassrooms;

    public ClassroomsEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node createContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #f8f9fa;");

        VBox headerArea = createSectionHeader("Навчальні аудиторії", "Керуйте списком кабінетів та приміщень школи", "#6c5ce7", ICON_ROOM);
        
        TextField addField = new TextField();
        addField.setPromptText("Введіть назву або номер кабінету (наприклад, Каб. 301)...");
        addField.setStyle(COMBO_STYLE);
        addField.setPrefWidth(550);

        Button addBtn = new Button("ДОДАТИ КАБІНЕТ");
        addBtn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8;");

        FlowPane classroomsContainer = new FlowPane(15, 15);
        classroomsContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(classroomsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshClassrooms = () -> {
            classroomsContainer.getChildren().clear();
            for (Classroom c : mainApp.getAcademicService().getAllClassrooms()) {
                VBox card = new VBox(8);
                card.setStyle("-fx-background-color: #f3efff; -fx-background-radius: 16; -fx-padding: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);");
                card.setPrefWidth(240);
                
                HBox header = new HBox(8);
                header.setAlignment(Pos.CENTER_LEFT);
                
                TextField edit = new TextField(c.name());
                edit.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 13px;");
                HBox.setHgrow(edit, Priority.ALWAYS);
                
                edit.focusedProperty().addListener((obs, ov, nv) -> {
                    if (!nv && !edit.getText().equals(c.name()) && !edit.getText().isEmpty()) {
                        mainApp.getAcademicService().updateClassroom(c.id(), edit.getText());
                        refreshClassrooms.run();
                    }
                });

                Button del = new Button("✕");
                del.setStyle("-fx-text-fill: #ff7675; -fx-background-color: transparent; -fx-cursor: hand;");
                del.setOnAction(e -> { 
                    mainApp.getAcademicService().deleteClassroom(c.id()); 
                    refreshClassrooms.run(); 
                });

                header.getChildren().addAll(createSVGIcon(ICON_ROOM, Color.web("#6c5ce7"), 16), edit, del);
                card.getChildren().add(header);
                classroomsContainer.getChildren().add(card);
            }
        };

        addBtn.setOnAction(e -> {
            if (!addField.getText().isEmpty()) {
                mainApp.getAcademicService().addClassroom(addField.getText());
                addField.clear();
                refreshClassrooms.run();
            }
        });

        content.getChildren().addAll(headerArea, new HBox(15, addField, addBtn), scroll);
        refreshClassrooms.run();
        return content;
    }
}
