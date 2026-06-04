package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.MediaEvent;
import com.schoolbell.service.MediaSchedulerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.File;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class MediaSchedulerPanel {
    private final MainApp mainApp;

    public MediaSchedulerPanel(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node build() {
        VBox card = new VBox(25);
        card.setPadding(new Insets(30));
        card.setStyle(SOFT_CARD);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(4);
        Label title = new Label("Автоматичні аудіо-повідомлення");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        Label subtitle = new Label("Керуйте розкладом автоматичного відтворення аудіо для шкільних повідомлень та подій.");
        subtitle.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + COLOR_SLATE + ";");
        copy.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = createSmallPrimaryActionButton("ДОДАТИ ПОВІДОМЛЕННЯ", ICON_PLUS);
        addBtn.setOnAction(e -> {
            new MediaEventEditorDialog(mainApp, null).showAndWait();
            refreshMediaEventsList((VBox) card.getChildren().get(1));
        });

        header.getChildren().addAll(copy, spacer, addBtn);

        VBox list = new VBox(15);
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
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20, 24, 20, 24));

        boolean pathExists = new File(event.path()).exists();
        
        String normalStyle = "-fx-background-color: white; -fx-background-radius: 22; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 22;";
        String normalHover = "-fx-background-color: " + COLOR_SURFACE_SUBTLE + "; -fx-background-radius: 22; -fx-border-color: " + COLOR_SLATE_MUTED + "; -fx-border-radius: 22;";
        String errorStyle = "-fx-background-color: " + COLOR_DANGER_PALE + "; -fx-background-radius: 22; -fx-border-color: " + COLOR_DANGER_BORDER + "; -fx-border-radius: 22;";
        String errorHover = "-fx-background-color: " + COLOR_DANGER_LIGHT + "; -fx-background-radius: 22; -fx-border-color: " + COLOR_DANGER + "40; -fx-border-radius: 22;";

        row.setStyle(pathExists ? normalStyle : errorStyle);
        row.setOnMouseEntered(e -> {
            if (pathExists) {
                row.setStyle(normalStyle + "-fx-border-color: " + COLOR_PRIMARY + "40;");
            } else {
                row.setStyle(errorStyle + "-fx-border-color: " + COLOR_DANGER + "60;");
            }
        });
        row.setOnMouseExited(e -> row.setStyle(pathExists ? normalStyle : errorStyle));

        VBox iconBox = new VBox(createSVGIcon(event.isFolder() ? ICON_FOLDER : ICON_MUSIC, Color.web(pathExists ? COLOR_PRIMARY : COLOR_DANGER), 22));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(52, 52);
        iconBox.setStyle(ICON_BADGE_STYLE + "-fx-background-radius: 14;");

        Label name = new Label(event.name());
        name.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: " + (pathExists ? COLOR_NAVY : COLOR_DANGER) + ";");
        
        Label detail = new Label(describeEvent(event));
        detail.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: " + COLOR_SLATE + ";");
        
        VBox info = new VBox(4, name, detail);
        HBox.setHgrow(info, Priority.ALWAYS);

        if (!pathExists) {
            Label errorLabel = new Label("УВАГА: Файл або папка не існує!");
            errorLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_DANGER + "; -fx-padding: 2 0;");
            info.getChildren().add(1, errorLabel);
        }

        Label status = new Label(event.isActive() ? "АКТИВНЕ" : "ВИМКНЕНЕ");
        status.setStyle(
                "-fx-font-family: 'Inter';" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 900;" +
                "-fx-text-fill: " + (event.isActive() ? COLOR_SUCCESS : COLOR_SLATE) + ";" +
                "-fx-background-color: " + (event.isActive() ? COLOR_SUCCESS_LIGHT : COLOR_SURFACE_SOFT) + ";" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 6 14;" +
                "-fx-border-color: " + (event.isActive() ? COLOR_SUCCESS_BORDER : COLOR_BORDER_SOFT) + ";" +
                "-fx-border-radius: 999;"
        );

        var toggle = createToggleSwitch(event.isActive());
        toggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            MediaEvent updated = new MediaEvent(event.id(), event.name(), event.path(), event.type(), event.time(), event.daysOfWeek(), event.date(), newVal, event.isFolder(), event.durationMinutes(), event.breakAnchor(), event.breakOffset());
            mainApp.getMediaSchedulerService().updateEvent(updated);
            row.setOpacity(newVal ? 1.0 : 0.75);
            status.setText(newVal ? "АКТИВНЕ" : "ВИМКНЕНЕ");
            status.setStyle(
                    "-fx-font-family: 'Inter';" +
                    "-fx-font-size: 10px;" +
                    "-fx-font-weight: 900;" +
                    "-fx-text-fill: " + (newVal ? COLOR_SUCCESS : COLOR_SLATE) + ";" +
                    "-fx-background-color: " + (newVal ? COLOR_SUCCESS_LIGHT : COLOR_SURFACE_SOFT) + ";" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 6 14;" +
                    "-fx-border-color: " + (newVal ? COLOR_SUCCESS_BORDER : COLOR_BORDER_SOFT) + ";" +
                    "-fx-border-radius: 999;"
            );
        });
        row.setOpacity(event.isActive() ? 1.0 : 0.75);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button edit = createCardActionButton(ICON_EDIT, COLOR_SURFACE_SUBTLE, COLOR_PRIMARY);
        edit.setOnAction(e -> {
            new MediaEventEditorDialog(mainApp, event).showAndWait();
            refreshMediaEventsList(list);
        });
        Button delete = createCardActionButton(ICON_TRASH, COLOR_DANGER_LIGHT, COLOR_DANGER);
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
