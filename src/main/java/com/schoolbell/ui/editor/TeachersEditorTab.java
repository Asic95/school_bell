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

import static com.schoolbell.ui.UIComponents.createSectionHeader;
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

        Button addBtn = new Button("ДОДАТИ ВЧИТЕЛЯ");
        String addBtnStyle = "-fx-background-color: #0984e3; -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 10 30; -fx-background-radius: 12; -fx-cursor: hand;";
        addBtn.setStyle(addBtnStyle);
        addBtn.setOnMouseEntered(e -> addBtn.setStyle(addBtnStyle + "-fx-background-color: #076ad2; -fx-effect: dropshadow(three-pass-box, rgba(9, 132, 227, 0.3), 15, 0, 0, 5);"));
        addBtn.setOnMouseExited(e -> addBtn.setStyle(addBtnStyle));

        FlowPane cardsContainer = new FlowPane(20, 20);
        cardsContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshTeachers = () -> {
            cardsContainer.getChildren().clear();
            List<Subject> allSubs = mainApp.getAcademicService().getAllSubjects();
            for (Teacher t : mainApp.getAcademicService().getAllTeachers()) {
                VBox card = new VBox(15);
                card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 12, 0, 0, 5); -fx-border-color: #f1f2f6; -fx-border-radius: 20;");
                card.setPrefWidth(340);

                HBox header = new HBox(10);
                header.setAlignment(Pos.CENTER_LEFT);
                StackPane avatar = com.schoolbell.ui.UIComponents.createAvatar(t.name(), 40);

                StackPane nameStack = new StackPane();
                Label nameLabel = new Label(t.name());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
                nameLabel.setWrapText(true);
                nameLabel.setMaxWidth(160);
                TextField nameEdit = new TextField(t.name());
                nameEdit.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                nameEdit.setVisible(false);
                nameStack.getChildren().addAll(nameLabel, nameEdit);
                StackPane.setAlignment(nameLabel, Pos.CENTER_LEFT);
                nameLabel.setOnMouseClicked(e -> { nameLabel.setVisible(false); nameEdit.setVisible(true); nameEdit.requestFocus(); });
                nameEdit.focusedProperty().addListener((obs, ov, nv) -> {
                    if (!nv) {
                        if (!nameEdit.getText().equals(t.name()) && !nameEdit.getText().isEmpty()) {
                            mainApp.getAcademicService().updateTeacher(t.id(), nameEdit.getText());
                            refreshTeachers.run();
                        } else {
                            nameEdit.setVisible(false);
                            nameLabel.setVisible(true);
                        }
                    }
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button edit = new Button();
                edit.setGraphic(createSVGIcon(ICON_EDIT, Color.web("#636e72"), 18));
                edit.setPrefSize(34, 34);
                String editStyle = "-fx-background-color: white; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-color: #dfe6e9; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);";
                edit.setStyle(editStyle);
                edit.setOnMouseEntered(e -> edit.setStyle(editStyle + "-fx-background-color: #f1f2f6; -fx-border-color: #b2bec3;"));
                edit.setOnMouseExited(e -> edit.setStyle(editStyle));
                edit.setOnAction(e -> { nameLabel.setVisible(false); nameEdit.setVisible(true); nameEdit.requestFocus(); });

                Button del = new Button();
                del.setGraphic(createSVGIcon(ICON_TRASH, Color.web("#ff7675"), 18));
                del.setPrefSize(34, 34);
                String delStyle = "-fx-background-color: white; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-color: #ffeaa7; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);";
                del.setStyle(delStyle);
                del.setOnMouseEntered(e -> del.setStyle(delStyle + "-fx-background-color: #fff5f5; -fx-border-color: #ff7675;"));
                del.setOnMouseExited(e -> del.setStyle(delStyle));
                del.setOnAction(e -> { mainApp.getAcademicService().deleteTeacher(t.id()); refreshTeachers.run(); });
                header.getChildren().addAll(avatar, nameStack, spacer, edit, del);

                FlowPane chips = new FlowPane(6, 6);
                for (Subject sub : mainApp.getAcademicService().getSubjectsForTeacher(t.id())) {
                    Label chip = new Label(sub.name() + " ✕");
                    chip.setStyle("-fx-background-color: #d1e8ff; -fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 11px; -fx-text-fill: #0984e3; -fx-cursor: hand;");
                    chip.setOnMouseEntered(e -> chip.setStyle("-fx-background-color: #ff7675; -fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 11px; -fx-text-fill: white; -fx-cursor: hand;"));
                    chip.setOnMouseExited(e -> chip.setStyle("-fx-background-color: #d1e8ff; -fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 11px; -fx-text-fill: #0984e3; -fx-cursor: hand;"));
                    chip.setOnMouseClicked(e -> { mainApp.getAcademicService().unlinkTeacherFromSubject(t.id(), sub.id()); refreshTeachers.run(); });
                    chips.getChildren().add(chip);
                }
                ComboBox<Subject> picker = new ComboBox<>();
                picker.setPromptText("+ додати предмет");
                picker.getItems().setAll(allSubs);
                picker.setMaxWidth(Double.MAX_VALUE);
                picker.setStyle("-fx-font-size: 11px; -fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #dfe6e9;");
                picker.setOnAction(e -> { if (picker.getValue() != null) { mainApp.getAcademicService().linkTeacherToSubject(t.id(), picker.getValue().id()); refreshTeachers.run(); } });
                card.getChildren().addAll(header, chips, picker);
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

        content.getChildren().addAll(headerArea, new HBox(15, addField, addBtn), scroll);
        refreshTeachers.run();
        return content;
    }
}
