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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UpdateAvailableDialog extends BasePremiumDialog {
    private final UpdateService updateService;
    private final UpdateManifest manifest;
    private final ProgressBar progressBar;
    private final Label statusLabel;

    private Runnable onSnoozeCallback;

    public UpdateAvailableDialog(Stage owner, UpdateService updateService, UpdateManifest manifest) {
        super(owner,
                manifest.critical() ? "КРИТИЧНЕ ОНОВЛЕННЯ СИСТЕМИ" : "ОНОВЛЕННЯ СИСТЕМИ",
                "Доступна версія " + manifest.latestVersion(),
                formatReleaseDate(manifest.releaseDate()),
                "ОНОВИТИ ЗАРАЗ",
                660);

        this.updateService = updateService;
        this.manifest = manifest;

        // Visual alert for critical updates
        if (manifest.critical()) {
            HBox criticalWarning = new HBox(12);
            criticalWarning.setAlignment(Pos.CENTER_LEFT);
            criticalWarning.setPadding(new Insets(12, 16, 12, 16));
            criticalWarning.setStyle("-fx-background-color: " + COLOR_DANGER_SOFT + "; " +
                    "-fx-border-color: " + COLOR_DANGER_BORDER + "; -fx-border-width: 1; -fx-background-radius: 14; -fx-border-radius: 14;");

            VBox iconBox = new VBox(createSVGIcon(ICON_ALERT, Color.web(COLOR_DANGER), 20));
            iconBox.setAlignment(Pos.CENTER);

            Label warnLabel = new Label("Це оновлення містить критичні виправлення стабільності та безпеки. Рекомендується встановити його негайно.");
            warnLabel.setWrapText(true);
            warnLabel.setStyle("-fx-text-fill: " + COLOR_DANGER + "; -fx-font-size: 13px; -fx-font-weight: 700;");
            HBox.setHgrow(warnLabel, Priority.ALWAYS);

            criticalWarning.getChildren().addAll(iconBox, warnLabel);
            content.getChildren().add(criticalWarning);
        }

        VBox changelogContainer = new VBox(12);

        Label changelogTitle = new Label("ЩО НОВОГО:");
        changelogTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_SLATE + "; -fx-letter-spacing: 1.5px;");
        changelogContainer.getChildren().add(changelogTitle);

        VBox list = new VBox(10);
        list.setPadding(new Insets(5, 12, 5, 0));

        List<String> items = new ArrayList<>();
        if (manifest.changelog() != null) {
            for (String raw : manifest.changelog()) {
                if (raw == null || raw.isBlank()) continue;
                String[] lines = raw.split("\\r?\\n");
                for (String line : lines) {
                    String clean = line.replaceFirst("^[\\s-*•–—]+", "").trim();
                    if (!clean.isEmpty()) {
                        items.add(clean);
                    }
                }
            }
        }

        // If changelog came as a single unformatted sentence block, intelligently split by sentences
        if (items.size() == 1) {
            String single = items.getFirst();
            String[] sentences = single.split("(?<=\\.)\\s+(?=[А-ЯA-ZІЇЄ])");
            if (sentences.length > 1) {
                items.clear();
                for (String s : sentences) {
                    String clean = s.replaceFirst("^[\\s-*•–—]+", "").trim();
                    if (!clean.isEmpty()) {
                        items.add(clean);
                    }
                }
            }
        }

        if (items.isEmpty()) {
            HBox emptyRow = new HBox(10);
            emptyRow.setAlignment(Pos.CENTER_LEFT);
            emptyRow.getChildren().addAll(
                    createSVGIcon(ICON_INFO, Color.web(COLOR_SLATE_MUTED), 14),
                    new Label("Список змін відсутній.")
            );
            list.getChildren().add(emptyRow);
        } else {
            for (String item : items) {
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
        }

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Adaptive height: accommodate items naturally without unnecessary scrollbars
        int itemCount = Math.max(1, items.size());
        double targetHeight = Math.clamp(itemCount * 42.0, 150, 360);
        scrollPane.setPrefHeight(targetHeight);
        scrollPane.setMaxHeight(380);

        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-width: 0 0 1 0;");

        // Modern sleek scrollbar styling (thin rounded bar, no clunky arrow buttons)
        String scrollbarCss = 
            ".scroll-pane { -fx-background-color: transparent; -fx-background: transparent; } " +
            ".scroll-pane > .viewport { -fx-background-color: transparent; } " +
            ".scroll-pane > .scroll-bar:vertical { -fx-background-color: transparent; -fx-pref-width: 7px; } " +
            ".scroll-pane > .scroll-bar:vertical > .track { -fx-background-color: transparent; } " +
            ".scroll-pane > .scroll-bar:vertical > .thumb { -fx-background-color: #cbd5e1; -fx-background-radius: 99; } " +
            ".scroll-pane > .scroll-bar:vertical > .thumb:hover { -fx-background-color: #94a3b8; } " +
            ".scroll-pane > .scroll-bar > .increment-button, .scroll-pane > .scroll-bar > .decrement-button { -fx-pref-height: 0; -fx-padding: 0; -fx-shape: \" \"; }";
        scrollPane.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(scrollbarCss.getBytes()));

        changelogContainer.getChildren().add(scrollPane);

        content.getChildren().add(changelogContainer);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setMinHeight(16);
        progressBar.setPrefHeight(16);
        progressBar.setMaxHeight(16);
        
        String barStyle = "-fx-background-color: linear-gradient(to right, #4f46e5, #7c3aed); -fx-background-radius: 99; -fx-background-insets: 0;";
        String trackStyle = "-fx-background-color: #f1f5f9; -fx-background-radius: 99; -fx-background-insets: 0; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 99;";
        
        progressBar.setStyle(
            "-fx-indeterminate-bar-length: 60; -fx-indeterminate-bar-escape: true; -fx-indeterminate-bar-flip: true; -fx-indeterminate-bar-animation-delay: 20;"
        );
        
        // CSS to target internal bar and track
        String premiumProgressCss = 
            ".progress-bar > .track { " + trackStyle + " } " +
            ".progress-bar > .bar { " + barStyle + " -fx-effect: dropshadow(three-pass-box, rgba(79, 70, 229, 0.3), 10, 0, 0, 2); }";
        
        progressBar.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(premiumProgressCss.getBytes()));
        
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_NAVY + ";");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        VBox progressContainer = new VBox(10, statusLabel, progressBar);
        progressContainer.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(progressContainer);

        // Change save button icon to update icon instead of default save icon
        saveBtn.setGraphic(createSVGIcon(ICON_UPDATE, Color.WHITE, 18));
        if (manifest.critical()) {
            saveBtn.setStyle(PREMIUM_BTN_STYLE + "-fx-background-color: " + GRADIENT_DANGER + ";");
        }

        // Configure cancel/snooze button
        cancelBtn.setText("ВІДКЛАСТИ");
        cancelBtn.setOnAction(e -> {
            long duration = manifest.critical() ? (2 * 3600 * 1000L) : (24 * 3600 * 1000L);
            updateService.snoozeUpdate(manifest, duration);
            if (onSnoozeCallback != null) {
                onSnoozeCallback.run();
            }
            close();
        });

        // Phase 2: Check if background download already completed or is in progress
        if (updateService.getDownloadStatus() == UpdateService.DownloadStatus.VERIFIED &&
                updateService.getVerifiedInstallerFile() != null &&
                updateService.getVerifiedInstallerFile().exists() &&
                manifest.latestVersion().equalsIgnoreCase(updateService.getDownloadingVersion())) {
            saveBtn.setText("ВСТАНОВИТИ ЗАРАЗ");
            saveBtn.setGraphic(createSVGIcon(ICON_CHECK, Color.WHITE, 18));
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            progressBar.setVisible(false);
            progressBar.setManaged(false);
            statusLabel.setText("Оновлення завантажено й готове до встановлення.");
            statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_SUCCESS + ";");
        } else if (updateService.getDownloadStatus() == UpdateService.DownloadStatus.DOWNLOADING &&
                manifest.latestVersion().equalsIgnoreCase(updateService.getDownloadingVersion())) {
            saveBtn.setText("ЗАВАНТАЖЕННЯ...");
            saveBtn.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            attachToOngoingDownload();
        }
    }

    public void setOnSnoozeCallback(Runnable callback) {
        this.onSnoozeCallback = callback;
    }

    private static String formatReleaseDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "Дата невідома";
        try {
            LocalDate date = LocalDate.parse(isoDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return "Реліз від " + date.format(formatter);
        } catch (Exception e) {
            return "Реліз від " + isoDate;
        }
    }

    @Override
    protected boolean onSave() {
        if (updateService.getDownloadStatus() == UpdateService.DownloadStatus.VERIFIED &&
                updateService.getVerifiedInstallerFile() != null &&
                updateService.getVerifiedInstallerFile().exists() &&
                manifest.latestVersion().equalsIgnoreCase(updateService.getDownloadingVersion())) {
            updateService.installUpdate(updateService.getVerifiedInstallerFile());
            return false;
        }
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

        updateService.startBackgroundDownload(manifest);
        attachToOngoingDownload();
    }

    private void attachToOngoingDownload() {
        java.util.function.Consumer<Double> progressListener = progress -> Platform.runLater(() -> {
            if (progress >= 0) {
                progressBar.setProgress(progress);
                statusLabel.setText(String.format("Завантаження: %.0f%%", progress * 100));
            } else {
                progressBar.setProgress(-1); // Indeterminate
                statusLabel.setText(String.format("Завантажено: %.1f MB", -progress));
            }
        });
        updateService.addProgressListener(progressListener);

        java.util.function.Consumer<UpdateService.DownloadStatus> statusListener = new java.util.function.Consumer<>() {
            @Override
            public void accept(UpdateService.DownloadStatus status) {
                Platform.runLater(() -> {
                    if (status == UpdateService.DownloadStatus.VERIFIED) {
                        saveBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                        saveBtn.setText("ВСТАНОВИТИ ЗАРАЗ");
                        saveBtn.setGraphic(createSVGIcon(ICON_CHECK, Color.WHITE, 18));
                        statusLabel.setText("Завантаження завершено. Оновлення готове до встановлення.");
                        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_SUCCESS + ";");
                        updateService.removeProgressListener(progressListener);
                        updateService.removeStatusListener(this);
                    } else if (status == UpdateService.DownloadStatus.FAILED) {
                        saveBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                        saveBtn.setText("СПРОБУВАТИ ЗНОВУ");
                        statusLabel.setText("Помилка завантаження оновлення.");
                        statusLabel.setStyle("-fx-text-fill: " + COLOR_DANGER + "; -fx-font-weight: bold;");
                        updateService.removeProgressListener(progressListener);
                        updateService.removeStatusListener(this);
                    }
                });
            }
        };
        updateService.addStatusListener(statusListener);

        setOnHidden(e -> {
            updateService.removeProgressListener(progressListener);
            updateService.removeStatusListener(statusListener);
        });
    }
}
