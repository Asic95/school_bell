package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.Classroom;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.createPageHeader;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
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

        HBox header = createPageHeader(
            "ІНФРАСТРУКТУРА",
            "Навчальні аудиторії",
            "Керуйте переліком кабінетів, лабораторій та залів вашої школи.",
            ICON_ROOM,
            "#00cec9",
            null
        );

        TextField addField = new TextField();
        addField.setPromptText("Введіть назву або номер кабінету (наприклад, Каб. 301)...");
        addField.setStyle(COMBO_STYLE);
        addField.setPrefWidth(550);

        Button addBtn = createPrimaryActionButton("ДОДАТИ КАБІНЕТ", ICON_PLUS);
        addBtn.setStyle(addBtn.getStyle().replace(COLOR_PRIMARY, "#00cec9"));

        FlowPane classroomsContainer = new FlowPane(20, 20);
        classroomsContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(classroomsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshClassrooms = () -> {
            classroomsContainer.getChildren().clear();
            for (Classroom c : mainApp.getAcademicService().getAllClassrooms()) {
                VBox card = new VBox(15);
                card.setStyle(SOFT_CARD + "-fx-padding: 20; -fx-border-color: #f1f2f6; -fx-border-radius: 20;");
                card.setPrefWidth(300);
                
                HBox topRow = new HBox(12);
                topRow.setAlignment(Pos.CENTER_LEFT);
                
                VBox iconBox = new VBox(createSVGIcon(ICON_ROOM, Color.web("#00cec9"), 18));
                iconBox.setAlignment(Pos.CENTER);
                iconBox.setPrefSize(40, 40);
                iconBox.setMinSize(40, 40);
                iconBox.setMaxSize(40, 40);
                iconBox.setStyle("-fx-background-color: #00cec915; -fx-background-radius: 12;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button editBtn = createCardActionButton(ICON_EDIT, "#f1f2f6", COLOR_PRIMARY);
                Button delBtn = createCardActionButton(ICON_TRASH, "#fff5f5", COLOR_DANGER);
                delBtn.setOnAction(e -> { 
                    mainApp.getAcademicService().deleteClassroom(c.id()); 
                    refreshClassrooms.run(); 
                });

                topRow.getChildren().addAll(iconBox, spacer, editBtn, delBtn);

                VBox nameArea = new VBox(5);
                Label nameLabel = new Label(c.name());
                nameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #2d3436;");
                nameLabel.setWrapText(true);
                nameLabel.setMaxWidth(260);
                
                TextField nameEdit = new TextField(c.name());
                nameEdit.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #f1f2f6; -fx-background-radius: 8; -fx-padding: 8 12;");
                nameEdit.setMaxWidth(Double.MAX_VALUE);
                nameEdit.setManaged(false);
                nameEdit.setVisible(false);
                
                nameArea.getChildren().addAll(nameLabel, nameEdit);

                editBtn.setOnAction(e -> {
                    nameLabel.setVisible(false); nameLabel.setManaged(false);
                    nameEdit.setVisible(true); nameEdit.setManaged(true);
                    nameEdit.requestFocus();
                    nameEdit.selectAll();
                });

                nameEdit.focusedProperty().addListener((obs, ov, nv) -> {
                    if (!nv) {
                        if (!nameEdit.getText().equals(c.name()) && !nameEdit.getText().isEmpty()) {
                            mainApp.getAcademicService().updateClassroom(c.id(), nameEdit.getText());
                            refreshClassrooms.run();
                        } else {
                            nameEdit.setVisible(false); nameEdit.setManaged(false);
                            nameLabel.setVisible(true); nameLabel.setManaged(true);
                        }
                    }
                });

                card.getChildren().addAll(topRow, nameArea);
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

        VBox contentLayout = new VBox(25, header, new HBox(15, addField, addBtn), scroll);
        contentLayout.setPadding(new Insets(30));
        contentLayout.setStyle("-fx-background-color: #f8f9fa;");

        refreshClassrooms.run();

        ScrollPane mainScroll = new ScrollPane(contentLayout);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }
}
