package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.Subject;
import com.schoolbell.model.Teacher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class TeachersEditorTab {
    private final MainApp mainApp;
    private Runnable refreshTeachers;

    public TeachersEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node createContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #f8f9fa;");

        VBox headerArea = createSectionHeader("Керування викладацьким складом", "Додавайте, редагуйте та призначайте предмети вчителям", "#0984e3", ICON_PERSON);

        TextField addField = new TextField();
        addField.setPromptText("Введіть ПІБ вчителя...");
        addField.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #dfe6e9; -fx-border-radius: 12; -fx-padding: 10 15;");
        addField.setPrefWidth(550);

        Button addBtn = createPrimaryActionButton("ДОДАТИ ВЧИТЕЛЯ", ICON_PLUS);
        addBtn.setStyle(addBtn.getStyle().replace(COLOR_PRIMARY, "#0984e3"));

        FlowPane cardsContainer = new FlowPane(20, 20);
        cardsContainer.setPadding(new Insets(10));

        refreshTeachers = () -> {
            cardsContainer.getChildren().clear();
            List<Subject> allSubs = mainApp.getAcademicService().getAllSubjects();
            for (Teacher t : mainApp.getAcademicService().getAllTeachers()) {
                VBox card = new VBox(15);
                card.setStyle(SOFT_CARD + "-fx-padding: 20; -fx-border-color: #f1f2f6; -fx-border-radius: 20;");
                card.setPrefWidth(340);

                HBox topRow = new HBox(12);
                topRow.setAlignment(Pos.CENTER_LEFT);
                StackPane avatar = com.schoolbell.ui.UIComponents.createAvatar(t.name(), 44);
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button edit = createCardActionButton(ICON_EDIT, "#f1f2f6", COLOR_PRIMARY);
                Button del = createCardActionButton(ICON_TRASH, "#fff5f5", COLOR_DANGER);
                del.setOnAction(e -> { mainApp.getAcademicService().deleteTeacher(t.id()); refreshTeachers.run(); });

                topRow.getChildren().addAll(avatar, spacer, edit, del);

                VBox nameArea = new VBox(5);
                Label nameLabel = new Label(t.name());
                nameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #2d3436;");
                nameLabel.setWrapText(true);
                nameLabel.setMaxWidth(300);
                
                TextField nameEdit = new TextField(t.name());
                nameEdit.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #f1f2f6; -fx-background-radius: 8; -fx-padding: 8 12;");
                nameEdit.setMaxWidth(Double.MAX_VALUE);
                nameEdit.setManaged(false);
                nameEdit.setVisible(false);
                
                nameArea.getChildren().addAll(nameLabel, nameEdit);

                edit.setOnAction(e -> { 
                    nameLabel.setVisible(false); nameLabel.setManaged(false);
                    nameEdit.setVisible(true); nameEdit.setManaged(true);
                    nameEdit.requestFocus();
                    nameEdit.selectAll();
                });

                nameEdit.focusedProperty().addListener((obs, ov, nv) -> {
                    if (!nv) {
                        if (!nameEdit.getText().equals(t.name()) && !nameEdit.getText().isEmpty()) {
                            mainApp.getAcademicService().updateTeacher(t.id(), nameEdit.getText());
                            refreshTeachers.run();
                        } else {
                            nameEdit.setVisible(false); nameEdit.setManaged(false);
                            nameLabel.setVisible(true); nameLabel.setManaged(true);
                        }
                    }
                });

                FlowPane chips = new FlowPane(6, 6);
                for (Subject sub : mainApp.getAcademicService().getSubjectsForTeacher(t.id())) {
                    Label chip = new Label(sub.name().toUpperCase() + " ✕");
                    String chipBase = "-fx-background-color: #e3f2fd; -fx-background-radius: 10; -fx-padding: 4 10; -fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #0984e3; -fx-cursor: hand; -fx-border-color: #0984e320; -fx-border-radius: 10;";
                    chip.setStyle(chipBase);
                    chip.setOnMouseEntered(e -> chip.setStyle(chipBase + "-fx-background-color: #ff767515; -fx-text-fill: #ff7675; -fx-border-color: #ff767540;"));
                    chip.setOnMouseExited(e -> chip.setStyle(chipBase));
                    chip.setOnMouseClicked(e -> { mainApp.getAcademicService().unlinkTeacherFromSubject(t.id(), sub.id()); refreshTeachers.run(); });
                    chips.getChildren().add(chip);
                }
                
                ComboBox<Subject> picker = new ComboBox<>();
                picker.setPromptText("+ ПРИЗНАЧИТИ ПРЕДМЕТ");
                picker.getItems().setAll(allSubs);
                picker.setMaxWidth(Double.MAX_VALUE);
                picker.setStyle(COMBO_STYLE + "-fx-font-size: 11px; -fx-font-weight: 900;");
                picker.setOnAction(e -> { if (picker.getValue() != null) { mainApp.getAcademicService().linkTeacherToSubject(t.id(), picker.getValue().id()); refreshTeachers.run(); } });
                
                card.getChildren().addAll(topRow, nameArea, chips, picker);
                cardsContainer.getChildren().add(card);
            }
        };

        addBtn.setOnAction(e -> {
            String teacherName = addField.getText().trim();
            if (!teacherName.isEmpty()) {
                mainApp.getAcademicService().addTeacher(teacherName);
                addField.clear();
                refreshTeachers.run();
            }
        });

        content.getChildren().addAll(headerArea, new HBox(15, addField, addBtn), cardsContainer);
        refreshTeachers.run();

        ScrollPane mainScroll = new ScrollPane(content);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }
}
