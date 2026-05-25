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
import static com.schoolbell.ui.ControlFactory.*;
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
        content.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + ";");

        HBox header = createPageHeader(
            "ІНФРАСТРУКТУРА",
            "Навчальні аудиторії",
            "Керуйте переліком кабінетів, лабораторій та залів вашої школи.",
            ICON_ROOM,
            COLOR_CYAN,
            null
        );

        TextField addField = createStyledField("");
        addField.setPromptText("Введіть назву або номер кабінету (наприклад, Каб. 301)...");
        addField.setPrefWidth(550);

        Button addBtn = createPrimaryActionButton("ДОДАТИ КАБІНЕТ", ICON_PLUS);
        addBtn.setStyle(PREMIUM_BTN_STYLE);

        FlowPane classroomsContainer = new FlowPane(20, 20);
        classroomsContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(classroomsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshClassrooms = () -> {
            classroomsContainer.getChildren().clear();
            java.util.List<Classroom> classrooms = mainApp.getAcademicService().getAllClassrooms();
            
            if (classrooms.isEmpty()) {
                VBox empty = new VBox(20);
                empty.setAlignment(Pos.CENTER);
                empty.setPadding(new Insets(100, 0, 100, 0));
                empty.setMinWidth(900); // Ensure it fills the area
                Node emptyIcon = createSVGIcon(ICON_INFO, Color.web(COLOR_WHITE_MUTED_BORDER), 64);
                Label emptyLabel = new Label("Список кабінетів порожній");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_ICON_MUTED + ";");
                Label subLabel = new Label("Використовуйте поле вище, щоб додати перший кабінет");
                subLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_SLATE + ";");
                empty.getChildren().addAll(emptyIcon, emptyLabel, subLabel);
                classroomsContainer.getChildren().add(empty);
            } else {
                for (Classroom c : classrooms) {
                    VBox card = new VBox(20);
                    card.setStyle(SOFT_CARD + "-fx-padding: 24;");
                    card.setPrefWidth(320);
                    
                    HBox topRow = new HBox(12);
                    topRow.setAlignment(Pos.CENTER_LEFT);
                    
                    VBox iconBox = new VBox(createSVGIcon(ICON_ROOM, Color.web(COLOR_PRIMARY), 22));
                    iconBox.setAlignment(Pos.CENTER);
                    iconBox.setPrefSize(52, 52);
                    iconBox.setMinSize(52, 52);
                    iconBox.setMaxSize(52, 52);
                    iconBox.setStyle(ICON_BADGE_STYLE);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button editBtn = createCardActionButton(ICON_EDIT, COLOR_SURFACE_SUBTLE, COLOR_PRIMARY);
                    Button delBtn = createCardActionButton(ICON_TRASH, COLOR_DANGER_LIGHT, COLOR_DANGER);
                    delBtn.setOnAction(e -> { 
                        mainApp.getAcademicService().deleteClassroom(c.id()); 
                        refreshClassrooms.run(); 
                    });

                    topRow.getChildren().addAll(iconBox, spacer, editBtn, delBtn);

                    VBox nameArea = new VBox(5);
                    Label nameLabel = new Label(c.name());
                    nameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 18px; -fx-text-fill: " + COLOR_NAVY + ";");
                    nameLabel.setWrapText(true);
                    nameLabel.setMaxWidth(260);
                    
                    TextField nameEdit = new TextField(c.name());
                    nameEdit.setStyle(PREMIUM_FIELD_STYLE + "-fx-font-size: 15px; -fx-padding: 8 12;");
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
        contentLayout.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + ";");

        refreshClassrooms.run();

        ScrollPane mainScroll = new ScrollPane(contentLayout);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }
}
