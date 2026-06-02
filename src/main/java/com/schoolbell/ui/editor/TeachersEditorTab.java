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

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.LayoutUtils.createAvatar;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class TeachersEditorTab {
    private final MainApp mainApp;
    private Runnable refreshTeachers;
    private String searchText = "";

    public TeachersEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node createContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + ";");

        HBox header = createPageHeader(
            "ПЕРСОНАЛ",
            "Викладацький склад",
            "Керування списком вчителів, їх спеціалізацією та закріпленими предметами.",
            ICON_PERSON,
            COLOR_SKY,
            null
        );

        TextField searchField = new TextField();
        searchField.setPromptText("Пошук вчителя або предмета...");
        searchField.setStyle(PREMIUM_FIELD_STYLE);
        searchField.setPrefWidth(350);
        searchField.textProperty().addListener((o, ov, nv) -> {
            this.searchText = nv.toLowerCase().trim();
            refreshTeachers.run();
        });

        TextField addField = createStyledField("");
        addField.setPromptText("Введіть ПІБ вчителя...");
        addField.setPrefWidth(400);

        Button addBtn = createPrimaryActionButton("ДОДАТИ ВЧИТЕЛЯ", ICON_PLUS);
        addBtn.setStyle(PREMIUM_BTN_STYLE);

        HBox actionsRow = new HBox(15, addField, addBtn, new Region(), searchField);
        HBox.setHgrow(actionsRow.getChildren().get(2), Priority.ALWAYS);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox cardsArea = new VBox();
        VBox.setVgrow(cardsArea, Priority.ALWAYS);

        FlowPane cardsContainer = new FlowPane(20, 20);
        cardsContainer.setPadding(new Insets(10));

        refreshTeachers = () -> {
            cardsArea.getChildren().clear();
            List<Subject> allSubs = mainApp.getStaffService().getAllSubjects();
            List<Teacher> allTeachers = mainApp.getStaffService().getAllTeachers();
            
            List<Teacher> teachers = allTeachers.stream()
                .filter(t -> {
                    if (searchText.isEmpty()) return true;
                    boolean matchesName = t.name().toLowerCase().contains(searchText);
                    boolean matchesSubject = mainApp.getStaffService().getSubjectsForTeacher(t.id()).stream()
                        .anyMatch(s -> s.name().toLowerCase().contains(searchText));
                    return matchesName || matchesSubject;
                })
                .toList();

            if (teachers.isEmpty()) {
                cardsArea.setAlignment(Pos.CENTER);
                if (allTeachers.isEmpty()) {
                    cardsArea.getChildren().add(createEmptyState(ICON_INFO, "Список вчителів порожній", "Введіть ім'я та натисніть кнопку, щоб додати першого вчителя"));
                } else {
                    cardsArea.getChildren().add(createEmptyState(ICON_SEARCH, "Вчителів не знайдено", "Спробуйте змінити параметри пошуку"));
                }
            } else {
                cardsArea.setAlignment(Pos.TOP_LEFT);
                cardsArea.getChildren().add(cardsContainer);
                cardsContainer.getChildren().clear();
                for (Teacher t : teachers) {
                    VBox card = new VBox(20);
                    card.setStyle(SOFT_CARD + "-fx-padding: 24;");
                    card.setPrefWidth(360);

                    HBox topRow = new HBox(12);
                    topRow.setAlignment(Pos.CENTER_LEFT);
                    StackPane avatar = createAvatar(t.name(), 48);
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button edit = createCardActionButton(ICON_EDIT, COLOR_SURFACE_SUBTLE, COLOR_PRIMARY);
                    Button del = createCardActionButton(ICON_TRASH, COLOR_DANGER_LIGHT, COLOR_DANGER);
                    del.setOnAction(e -> { mainApp.getStaffService().deleteTeacher(t.id()); refreshTeachers.run(); });

                    topRow.getChildren().addAll(avatar, spacer, edit, del);

                    VBox nameArea = new VBox(5);
                    Label nameLabel = new Label(t.name());
                    nameLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 17px; -fx-text-fill: " + COLOR_NAVY + ";");
                    nameLabel.setWrapText(true);
                    nameLabel.setMaxWidth(300);
                    
                    TextField nameEdit = new TextField(t.name());
                    nameEdit.setStyle(PREMIUM_FIELD_STYLE + "-fx-font-size: 15px; -fx-padding: 8 12;");
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
                                mainApp.getStaffService().updateTeacher(t.id(), nameEdit.getText());
                                refreshTeachers.run();
                            } else {
                                nameEdit.setVisible(false); nameEdit.setManaged(false);
                                nameLabel.setVisible(true); nameLabel.setManaged(true);
                            }
                        }
                    });

                    FlowPane chips = new FlowPane(6, 6);
                    for (Subject sub : mainApp.getStaffService().getSubjectsForTeacher(t.id())) {
                        Label chip = new Label(sub.name().toUpperCase() + " ✕");
                        String chipBase = "-fx-background-color: " + COLOR_BLUE_LIGHT + "; -fx-background-radius: 12; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SKY + "; -fx-cursor: hand; -fx-border-color: " + COLOR_SKY + "20; -fx-border-radius: 12;";
                        chip.setStyle(chipBase);
                        chip.setOnMouseEntered(e -> chip.setStyle(chipBase + "-fx-background-color: " + COLOR_SALMON + "15; -fx-text-fill: " + COLOR_SALMON + "; -fx-border-color: " + COLOR_SALMON + "40;"));
                        chip.setOnMouseExited(e -> chip.setStyle(chipBase));
                        chip.setOnMouseClicked(e -> { mainApp.getStaffService().unlinkTeacherFromSubject(t.id(), sub.id()); refreshTeachers.run(); });
                        chips.getChildren().add(chip);
                    }
                    
                    ComboBox<Subject> picker = new ComboBox<>();
                    picker.setPromptText("+ ПРИЗНАЧИТИ ПРЕДМЕТ");
                    picker.getItems().setAll(allSubs);
                    picker.setMaxWidth(Double.MAX_VALUE);
                    picker.setStyle(PREMIUM_SELECT_STYLE + "-fx-font-size: 13px; -fx-padding: 8 12;");
                    picker.setOnAction(e -> { if (picker.getValue() != null) { mainApp.getStaffService().linkTeacherToSubject(t.id(), picker.getValue().id()); refreshTeachers.run(); } });
                    
                    card.getChildren().addAll(topRow, nameArea, chips, picker);
                    cardsContainer.getChildren().add(card);
                }
            }
        };

        addBtn.setOnAction(e -> {
            String teacherName = addField.getText().trim();
            if (!teacherName.isEmpty()) {
                mainApp.getStaffService().addTeacher(teacherName);
                addField.clear();
                refreshTeachers.run();
            }
        });

        content.getChildren().addAll(header, actionsRow, cardsArea);
        refreshTeachers.run();

        ScrollPane mainScroll = new ScrollPane(content);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }
}
