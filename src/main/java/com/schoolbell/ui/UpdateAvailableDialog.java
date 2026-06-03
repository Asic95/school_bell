package com.schoolbell.ui;

import com.schoolbell.service.UpdateService;
import com.schoolbell.service.UpdateService.UpdateManifest;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class UpdateAvailableDialog extends BasePremiumDialog {
    private final UpdateService updateService;
    private final UpdateManifest manifest;
    private final ProgressBar progressBar;
    private final Label statusLabel;

    public UpdateAvailableDialog(Stage owner, UpdateService updateService, UpdateManifest manifest) {
        super(owner,
                "ОНОВЛЕННЯ СИСТЕМИ",
                "Доступна версія " + manifest.latestVersion(),
                "Реліз від " + manifest.releaseDate(),
                "ОНОВИТИ ЗАРАЗ",
                600);

        this.updateService = updateService;
        this.manifest = manifest;

        VBox changelogContainer = new VBox(12);
        
        Label changelogTitle = new Label("ЩО НОВОГО:");
        changelogTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_SLATE + "; -fx-letter-spacing: 1.5px;");
        changelogContainer.getChildren().add(changelogTitle);

        VBox list = new VBox(10);
        list.setPadding(new Insets(5, 0, 5, 0));
        for (String item : manifest.changelog()) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.TOP_LEFT);
            
            VBox bullet = new VBox(createSVGIcon(ICON_CHECK, Color.web(COLOR_PRIMARY), 12));
            bullet.setPadding(new Insets(4, 0, 0, 0));
            
            Label text = new Label(item);
            text.setWrapText(true);
            text.setStyle("-fx-text-fill: " + COLOR_NAVY + "; -fx-font-size: 14px; -fx-font-weight: 500;");
            HBox.setHgrow(text, Priority.ALWAYS);
            
            row.getChildren().addAll(bullet, text);
            list.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(180);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-width: 0 0 1 0;");
        changelogContainer.getChildren().add(scrollPane);

        content.getChildren().add(changelogContainer);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + COLOR_PRIMARY + ";");
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_SLATE + ";");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        content.getChildren().addAll(progressBar, statusLabel);
    }

    @Override
    protected boolean onSave() {
        startDownload();
        return false; // Stay open during download
    }

    private void startDownload() {
        saveBtn.setDisable(true);
        cancelBtn.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.setText("Підготовка до завантаження...");

        updateService.downloadUpdate(manifest, progress -> {
            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                statusLabel.setText(String.format("Завантаження: %.0f%%", progress * 100));
            });
        }).thenAccept(file -> {
            Platform.runLater(() -> {
                statusLabel.setText("Завантаження завершено. Запуск інсталятора...");
                updateService.installUpdate(file);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                saveBtn.setDisable(false);
                cancelBtn.setDisable(false);
                statusLabel.setText("Помилка: " + ex.getCause().getMessage());
                statusLabel.setStyle("-fx-text-fill: " + COLOR_DANGER + "; -fx-font-weight: bold;");
            });
            return null;
        });
    }
}
