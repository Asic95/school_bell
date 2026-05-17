package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.Subject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.UIComponents.*;
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

        Button addBtn = createPrimaryActionButton("ДОДАТИ ПРЕДМЕТ", ICON_PLUS);
        addBtn.setStyle(addBtn.getStyle().replace(COLOR_PRIMARY, "#00b894"));

        FlowPane subjectsContainer = new FlowPane(20, 20);
        subjectsContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(subjectsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshSubjects = () -> {
            subjectsContainer.getChildren().clear();
            for (Subject s : mainApp.getAcademicService().getAllSubjects()) {
                VBox card = new VBox(15);
                card.setStyle(SOFT_CARD + "-fx-padding: 20; -fx-border-color: #f1f2f6; -fx-border-radius: 20;");
                card.setPrefWidth(300);
                
                HBox topRow = new HBox(12);
                topRow.setAlignment(Pos.CENTER_LEFT);
                
                VBox iconBox = new VBox(createSVGIcon(ICON_BOOK, Color.web("#00b894"), 18));
                iconBox.setAlignment(Pos.CENTER);
                iconBox.setPrefSize(40, 40);
                iconBox.setMinSize(40, 40);
                iconBox.setMaxSize(40, 40);
                iconBox.setStyle("-fx-background-color: #00b89415; -fx-background-radius: 12;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button editBtn = createCardActionButton(ICON_EDIT, "#f1f2f6", COLOR_PRIMARY);
                Button del = createCardActionButton(ICON_TRASH, "#fff5f5", COLOR_DANGER);
                del.setOnAction(e -> { mainApp.getAcademicService().deleteSubject(s.id()); refreshSubjects.run(); });
                
                topRow.getChildren().addAll(iconBox, spacer, editBtn, del);

                VBox nameArea = new VBox(5);
                Label nameLabel = new Label(s.name());
                nameLabel.setWrapText(true);
                nameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #2d3436;");
                nameLabel.setMaxWidth(260);
                
                TextField nameEdit = new TextField(s.name());
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
                        if (!nameEdit.getText().equals(s.name()) && !nameEdit.getText().isEmpty()) {
                            mainApp.getAcademicService().updateSubject(s.id(), nameEdit.getText());
                            refreshSubjects.run();
                        } else {
                            nameEdit.setVisible(false); nameEdit.setManaged(false);
                            nameLabel.setVisible(true); nameLabel.setManaged(true);
                        }
                    }
                });

                card.getChildren().addAll(topRow, nameArea);
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

        ScrollPane mainScroll = new ScrollPane(content);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }
}
