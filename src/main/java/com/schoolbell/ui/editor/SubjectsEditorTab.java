package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.Subject;
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

public class SubjectsEditorTab {
    private final MainApp mainApp;
    private Runnable refreshSubjects;

    public SubjectsEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node createContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + ";");

        HBox header = createPageHeader(
            "ДОВІДНИК",
            "Навчальні дисципліни",
            "Керуйте переліком предметів, які викладаються у вашому закладі.",
            ICON_BOOK,
            COLOR_TEAL,
            null
        );

        TextField addField = createStyledField("");
        addField.setPromptText("Введіть назву предмета...");
        addField.setPrefWidth(550);

        Button addBtn = createPrimaryActionButton("ДОДАТИ ПРЕДМЕТ", ICON_PLUS);
        addBtn.setStyle(PREMIUM_BTN_STYLE);

        FlowPane subjectsContainer = new FlowPane(20, 20);
        subjectsContainer.setPadding(new Insets(10));

        refreshSubjects = () -> {
            subjectsContainer.getChildren().clear();
            java.util.List<Subject> subjects = mainApp.getStaffService().getAllSubjects();
            
            if (subjects.isEmpty()) {
                VBox empty = new VBox(20);
                empty.setAlignment(Pos.CENTER);
                empty.setPadding(new Insets(100, 0, 100, 0));
                empty.setMinWidth(900);
                Node emptyIcon = createSVGIcon(ICON_INFO, Color.web(COLOR_WHITE_MUTED_BORDER), 64);
                Label emptyLabel = new Label("Список предметів порожній");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_ICON_MUTED + ";");
                Label subLabel = new Label("Введіть назву та натисніть кнопку, щоб додати перший предмет");
                subLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_SLATE + ";");
                empty.getChildren().addAll(emptyIcon, emptyLabel, subLabel);
                subjectsContainer.getChildren().add(empty);
            } else {
                for (Subject s : subjects) {
                    VBox card = new VBox(20);
                    card.setStyle(SOFT_CARD + "-fx-padding: 24;");
                    card.setPrefWidth(320);
                    
                    HBox topRow = new HBox(12);
                    topRow.setAlignment(Pos.CENTER_LEFT);
                    
                    VBox iconBox = new VBox(createSVGIcon(ICON_BOOK, Color.web(COLOR_PRIMARY), 22));
                    iconBox.setAlignment(Pos.CENTER);
                    iconBox.setPrefSize(52, 52);
                    iconBox.setMinSize(52, 52);
                    iconBox.setMaxSize(52, 52);
                    iconBox.setStyle(ICON_BADGE_STYLE);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button editBtn = createCardActionButton(ICON_EDIT, COLOR_SURFACE_SUBTLE, COLOR_PRIMARY);
                Button del = createCardActionButton(ICON_TRASH, COLOR_DANGER_LIGHT, COLOR_DANGER);
                del.setOnAction(e -> { mainApp.getStaffService().deleteSubject(s.id()); refreshSubjects.run(); });
                
                topRow.getChildren().addAll(iconBox, spacer, editBtn, del);

                VBox nameArea = new VBox(5);
                Label nameLabel = new Label(s.name());
                nameLabel.setWrapText(true);
                nameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 17px; -fx-text-fill: " + COLOR_NAVY + ";");
                nameLabel.setMaxWidth(260);
                
                TextField nameEdit = new TextField(s.name());
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
                        if (!nameEdit.getText().equals(s.name()) && !nameEdit.getText().isEmpty()) {
                            mainApp.getStaffService().updateSubject(s.id(), nameEdit.getText());
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
        }
    };

        addBtn.setOnAction(e -> {
            if (!addField.getText().isEmpty()) {
                mainApp.getStaffService().addSubject(addField.getText());
                addField.clear();
                refreshSubjects.run();
            }
        });
        content.getChildren().addAll(header, new HBox(15, addField, addBtn), subjectsContainer);
        refreshSubjects.run();

        ScrollPane mainScroll = new ScrollPane(content);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }
}
