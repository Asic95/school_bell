package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.MediaEvent;
import com.schoolbell.service.MediaSchedulerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createStyledField;
import static com.schoolbell.ui.ControlFactory.createToggleSwitch;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.BTN_BASE;
import static com.schoolbell.ui.UIStyles.COLOR_BG;
import static com.schoolbell.ui.UIStyles.COLOR_PRIMARY;
import static com.schoolbell.ui.UIStyles.COLOR_SUCCESS;
import static com.schoolbell.ui.UIStyles.HEADER_STYLE;
import static com.schoolbell.ui.UIStyles.ICON_EDIT;
import static com.schoolbell.ui.UIStyles.ICON_FOLDER;
import static com.schoolbell.ui.UIStyles.ICON_MUSIC;
import static com.schoolbell.ui.UIStyles.ICON_PLUS;
import static com.schoolbell.ui.UIStyles.ICON_TRASH;
import static com.schoolbell.ui.UIStyles.MODERN_DATE_PICKER_STYLE;

public class MediaSchedulerPanel {
    private static final String SECTION_CARD =
            "-fx-background-color: rgba(255,255,255,0.96);" +
            "-fx-background-radius: 32;" +
            "-fx-border-color: rgba(226,232,240,0.72);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 32;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.08), 30, 0, 0, 10);";
    private static final String ITEM_CARD =
            "-fx-background-color: linear-gradient(to bottom right, #ffffff, #fbfdff);" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: rgba(226,232,240,0.65);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 22;";

    private final MainApp mainApp;

    public MediaSchedulerPanel(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node build() {
        VBox card = new VBox(20);
        card.setPadding(new Insets(28));
        card.setStyle(SECTION_CARD);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(4);
        Label title = new Label("Автоматичні аудіо-повідомлення");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Керуйте розкладом автоматичного відтворення аудіо для шкільних повідомлень та подій.");
        subtitle.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #64748b;");
        copy.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = createPrimaryActionButton("Додати повідомлення", ICON_PLUS);
        addBtn.setOnAction(e -> {
            new MediaEventEditorDialog(mainApp, null).showAndWait();
            refreshMediaEventsList((VBox) card.getChildren().get(1));
        });

        header.getChildren().addAll(copy, spacer, addBtn);

        VBox list = new VBox(14);
        refreshMediaEventsList(list);

        card.getChildren().addAll(header, list);
        return card;
    }

    private void refreshMediaEventsList(VBox list) {
        list.getChildren().clear();
        MediaSchedulerService service = mainApp.getMediaSchedulerService();
        for (MediaEvent event : service.getEvents()) {
            list.getChildren().add(createMediaEventCard(event, list));
        }
    }

    private HBox createMediaEventCard(MediaEvent event, VBox list) {
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18, 20, 18, 20));
        row.setStyle(ITEM_CARD);

        VBox iconBox = new VBox(createSVGIcon(event.isFolder() ? ICON_FOLDER : ICON_MUSIC, Color.web("#4f46e5"), 20));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(52, 52);
        iconBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #eef2ff, #eff6ff); -fx-background-radius: 18;");

        Label name = new Label(event.name());
        name.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label detail = new Label(describeEvent(event));
        detail.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #64748b;");
        VBox info = new VBox(4, name, detail);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label status = new Label(event.isActive() ? "Активне" : "Вимкнене");
        status.setStyle(
                "-fx-font-family: 'Inter';" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 700;" +
                "-fx-text-fill: " + (event.isActive() ? "#15803d" : "#64748b") + ";" +
                "-fx-background-color: " + (event.isActive() ? "#ecfdf3" : "#f1f5f9") + ";" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 6 12;"
        );

        var toggle = createToggleSwitch(event.isActive());
        toggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            MediaEvent updated = new MediaEvent(event.id(), event.name(), event.path(), event.type(), event.time(), event.daysOfWeek(), event.date(), newVal, event.isFolder(), event.durationMinutes(), event.breakAnchor(), event.breakOffset());
            mainApp.getMediaSchedulerService().updateEvent(updated);
            row.setOpacity(newVal ? 1.0 : 0.72);
            status.setText(newVal ? "Активне" : "Вимкнене");
            status.setStyle(
                    "-fx-font-family: 'Inter';" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: 700;" +
                    "-fx-text-fill: " + (newVal ? "#15803d" : "#64748b") + ";" +
                    "-fx-background-color: " + (newVal ? "#ecfdf3" : "#f1f5f9") + ";" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 6 12;"
            );
        });
        row.setOpacity(event.isActive() ? 1.0 : 0.72);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button edit = createCardActionButton(ICON_EDIT, "#EEF2FF", "#C7D2FE");
        edit.setOnAction(e -> {
            new MediaEventEditorDialog(mainApp, event).showAndWait();
            refreshMediaEventsList(list);
        });
        Button delete = createCardActionButton(ICON_TRASH, "#FEF2F2", "#FECACA");
        delete.setOnAction(e -> {
            mainApp.getMediaSchedulerService().deleteEvent(event.id());
            refreshMediaEventsList(list);
        });

        actions.getChildren().addAll(status, toggle, edit, delete);
        row.getChildren().addAll(iconBox, info, actions);
        return row;
    }

    private String describeEvent(MediaEvent event) {
        return switch (event.type()) {
            case "BREAKS" -> {
                String anchorText = switch (event.breakAnchor() != null ? event.breakAnchor() : "START") {
                    case "START" -> "початок перерви";
                    case "END" -> "кінець перерви";
                    case "MIDDLE" -> "середина перерви";
                    case "OFFSET" -> "зміщення на " + event.breakOffset() + " хв.";
                    default -> "початок перерви";
                };
                yield "Відтворення: " + anchorText;
            }
            case "TIME" -> "Щодня о " + event.time();
            case "ONCE" -> {
                try {
                    String[] parts = event.date().split("-");
                    yield "Разово: " + parts[2] + "." + parts[1] + "." + parts[0] + " о " + event.time();
                } catch (Exception e) {
                    yield "Разово: " + event.date() + " о " + event.time();
                }
            }
            default -> "";
        };
    }
}
