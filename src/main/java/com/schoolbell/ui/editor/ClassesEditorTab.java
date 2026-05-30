package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.SchoolClass;
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

public class ClassesEditorTab {
    private final MainApp mainApp;
    private Runnable refreshClasses;

    public ClassesEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node createContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + ";");

        HBox header = createPageHeader(
            "СТРУКТУРА",
            "Шкільні класи",
            "Керуйте переліком класів та паралелей вашого навчального закладу.",
            ICON_CLASS,
            COLOR_PURPLE,
            null
        );

        TextField addField = createStyledField("");
        addField.setPromptText("Назва класу (напр. 5-А)...");
        addField.setPrefWidth(550);

        Button addBtn = createPrimaryActionButton("ДОДАТИ КЛАС", ICON_PLUS);
        addBtn.setStyle(PREMIUM_BTN_STYLE);

        VBox cardsArea = new VBox();
        VBox.setVgrow(cardsArea, Priority.ALWAYS);

        FlowPane listContainer = new FlowPane(20, 20);
        listContainer.setPadding(new Insets(10));

        refreshClasses = () -> {
            cardsArea.getChildren().clear();
            java.util.List<SchoolClass> classes = mainApp.getAcademicService().getAllClasses();
            
            if (classes.isEmpty()) {
                cardsArea.setAlignment(Pos.CENTER);
                cardsArea.getChildren().add(createEmptyState(ICON_INFO, "Список класів порожній", "Введіть назву (напр. 5-А) та натисніть кнопку, щоб додати перший клас"));
            } else {
                cardsArea.setAlignment(Pos.TOP_LEFT);
                cardsArea.getChildren().add(listContainer);
                listContainer.getChildren().clear();
                for (SchoolClass c : classes) {
                    VBox card = new VBox(20);
                    card.setStyle(SOFT_CARD + "-fx-padding: 24;");
                    card.setPrefWidth(320);
                    
                    HBox topRow = new HBox(12);
                    topRow.setAlignment(Pos.CENTER_LEFT);
                    
                    VBox iconBox = new VBox(createSVGIcon(ICON_CLASS, Color.web(COLOR_PRIMARY), 22));
                    iconBox.setAlignment(Pos.CENTER);
                    iconBox.setPrefSize(52, 52);
                    iconBox.setMinSize(52, 52);
                    iconBox.setMaxSize(52, 52);
                    iconBox.setStyle(ICON_BADGE_STYLE);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button editBtn = createCardActionButton(ICON_EDIT, COLOR_SURFACE_SUBTLE, COLOR_PRIMARY);
                    Button del = createCardActionButton(ICON_TRASH, COLOR_DANGER_LIGHT, COLOR_DANGER);
                    del.setOnAction(e -> { mainApp.getAcademicService().deleteClass(c.id()); refreshClasses.run(); });

                    topRow.getChildren().addAll(iconBox, spacer, editBtn, del);

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
                                mainApp.getAcademicService().updateClass(c.id(), nameEdit.getText());
                                refreshClasses.run();
                            } else {
                                nameEdit.setVisible(false); nameEdit.setManaged(false);
                                nameLabel.setVisible(true); nameLabel.setManaged(true);
                            }
                        }
                    });

                    card.getChildren().addAll(topRow, nameArea);
                    listContainer.getChildren().add(card);
                }
            }
        };

        addBtn.setOnAction(e -> {
            if (!addField.getText().isEmpty()) {
                mainApp.getAcademicService().addClass(addField.getText());
                addField.clear();
                refreshClasses.run();
            }
        });

        VBox contentLayout = new VBox(25, header, new HBox(15, addField, addBtn), cardsArea);
        contentLayout.setPadding(new Insets(30));
        contentLayout.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + ";");

        refreshClasses.run();
        
        ScrollPane mainScroll = new ScrollPane(contentLayout);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }
}
