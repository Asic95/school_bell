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
import static com.schoolbell.ui.ControlFactory.createPageHeader;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
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
        content.setStyle("-fx-background-color: #f8f9fa;");

        HBox header = createPageHeader(
            "СТРУКТУРА",
            "Шкільні класи",
            "Керуйте переліком класів та паралелей вашого навчального закладу.",
            ICON_CLASS,
            "#a29bfe",
            null
        );

        TextField addField = new TextField();
        addField.setPromptText("Назва класу (напр. 5-А)...");
        addField.setStyle(COMBO_STYLE);
        addField.setPrefWidth(550);

        Button addBtn = createPrimaryActionButton("ДОДАТИ КЛАС", ICON_PLUS);
        addBtn.setStyle(addBtn.getStyle().replace(COLOR_PRIMARY, "#a29bfe"));

        FlowPane listContainer = new FlowPane(20, 20);
        listContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshClasses = () -> {
            listContainer.getChildren().clear();
            for (SchoolClass c : mainApp.getAcademicService().getAllClasses()) {
                VBox card = new VBox(15);
                card.setStyle(SOFT_CARD + "-fx-padding: 15; -fx-border-color: #f1f2f6; -fx-border-radius: 20;");
                card.setPrefWidth(200);
                
                HBox topRow = new HBox(10);
                topRow.setAlignment(Pos.CENTER_LEFT);
                
                VBox iconBox = new VBox(createSVGIcon(ICON_CLASS, Color.web(COLOR_PURPLE), 18));
                iconBox.setAlignment(Pos.CENTER);
                iconBox.setPrefSize(36, 36);
                iconBox.setMinSize(36, 36);
                iconBox.setMaxSize(36, 36);
                iconBox.setStyle("-fx-background-color: " + COLOR_PURPLE + "15; -fx-background-radius: 10;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button editBtn = createCardActionButton(ICON_EDIT, "#f1f2f6", COLOR_PRIMARY);
                editBtn.setPrefSize(30, 30); editBtn.setMinSize(30, 30);
                
                Button del = createCardActionButton(ICON_TRASH, "#fff5f5", COLOR_DANGER);
                del.setPrefSize(30, 30); del.setMinSize(30, 30);
                del.setOnAction(e -> { mainApp.getAcademicService().deleteClass(c.id()); refreshClasses.run(); });

                topRow.getChildren().addAll(iconBox, spacer, editBtn, del);

                VBox nameArea = new VBox(5);
                Label nameLabel = new Label(c.name());
                nameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #2d3436;");
                
                TextField nameEdit = new TextField(c.name());
                nameEdit.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #f1f2f6; -fx-background-radius: 8; -fx-padding: 6 10;");
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
        };

        addBtn.setOnAction(e -> {
            if (!addField.getText().isEmpty()) {
                mainApp.getAcademicService().addClass(addField.getText());
                addField.clear();
                refreshClasses.run();
            }
        });

        VBox contentLayout = new VBox(25, header, new HBox(15, addField, addBtn), scroll);
        contentLayout.setPadding(new Insets(30));
        contentLayout.setStyle("-fx-background-color: #f8f9fa;");

        refreshClasses.run();
        
        ScrollPane mainScroll = new ScrollPane(contentLayout);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }
}
