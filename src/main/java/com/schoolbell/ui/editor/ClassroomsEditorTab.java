package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.Classroom;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class ClassroomsEditorTab {
    private final MainApp mainApp;
    private Runnable refreshClassrooms;
    private String searchText = "";

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

        TextField searchField = new TextField();
        searchField.setPromptText("Пошук кабінету...");
        searchField.setStyle(PREMIUM_FIELD_STYLE);
        searchField.setPrefWidth(350);
        searchField.textProperty().addListener((o, ov, nv) -> {
            this.searchText = nv.toLowerCase().trim();
            refreshClassrooms.run();
        });

        TextField addField = createStyledField("");
        addField.setPromptText("Введіть назву або номер кабінету (напр. 301)...");
        addField.setPrefWidth(400);

        Button addBtn = createPrimaryActionButton("ДОДАТИ КАБІНЕТ", ICON_PLUS);
        addBtn.setStyle(PREMIUM_BTN_STYLE);

        HBox actionsRow = new HBox(15, addField, addBtn, new Region(), searchField);
        HBox.setHgrow(actionsRow.getChildren().get(2), Priority.ALWAYS);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox cardsArea = new VBox();
        VBox.setVgrow(cardsArea, Priority.ALWAYS);

        FlowPane classroomsContainer = new FlowPane(20, 20);
        classroomsContainer.setPadding(new Insets(10));

        refreshClassrooms = () -> {
            cardsArea.getChildren().clear();
            List<Classroom> allClassrooms = mainApp.getAcademicService().getAllClassrooms();
            
            List<Classroom> classrooms = allClassrooms.stream()
                .filter(c -> searchText.isEmpty() || c.name().toLowerCase().contains(searchText))
                .toList();

            if (classrooms.isEmpty()) {
                cardsArea.setAlignment(Pos.CENTER);
                if (allClassrooms.isEmpty()) {
                    cardsArea.getChildren().add(createEmptyState(ICON_INFO, "Список кабінетів порожній", "Використовуйте поле вище, щоб додати перший кабінет"));
                } else {
                    cardsArea.getChildren().add(createEmptyState(ICON_SEARCH, "Кабінетів не знайдено", "Спробуйте змінити запит"));
                }
            } else {
                cardsArea.setAlignment(Pos.TOP_LEFT);
                cardsArea.getChildren().add(classroomsContainer);
                classroomsContainer.getChildren().clear();
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

        VBox contentLayout = new VBox(25, header, actionsRow, cardsArea);
        contentLayout.setPadding(new Insets(30));
        contentLayout.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + ";");

        refreshClassrooms.run();

        ScrollPane mainScroll = new ScrollPane(contentLayout);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }
}
