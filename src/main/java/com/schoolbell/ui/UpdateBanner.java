package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.UpdateService;
import com.schoolbell.service.UpdateService.DownloadStatus;
import com.schoolbell.service.UpdateService.UpdateManifest;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

/**
 * Modern Academic Soft UI v2 persistent update banner with Phase 2 background download support.
 * Displays update availability and background download progression prominently
 * without blocking active school operations (bells, audio, alerts).
 */
public class UpdateBanner extends HBox {
    private final MainApp mainApp;
    private final UpdateService updateService;

    private UpdateManifest currentManifest;
    private final VBox iconBadge;
    private final Label eyebrowLabel;
    private final Label versionBadge;
    private final Label messageLabel;
    private final Button updateBtn;
    private final Button snoozeBtn;
    private final Button closeBtn;

    public UpdateBanner(MainApp mainApp, UpdateService updateService) {
        this.mainApp = mainApp;
        this.updateService = updateService;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setPadding(new Insets(12, 20, 12, 20));
        VBox.setMargin(this, new Insets(12, 20, 4, 20));

        // Default hidden
        setVisible(false);
        setManaged(false);
        setOpacity(0.0);

        // 1. Icon Badge
        iconBadge = new VBox();
        iconBadge.setAlignment(Pos.CENTER);
        iconBadge.setPrefSize(44, 44);
        iconBadge.setMinSize(44, 44);
        iconBadge.setMaxSize(44, 44);
        iconBadge.setStyle("-fx-background-radius: 14;");

        // 2. Text Container
        VBox textContainer = new VBox(3);
        textContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        eyebrowLabel = new Label();
        eyebrowLabel.setStyle(PREMIUM_LABEL_STYLE);

        versionBadge = new Label();
        versionBadge.setStyle("-fx-background-color: white; -fx-background-radius: 99; -fx-padding: 1 8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 99; -fx-text-fill: " + COLOR_NAVY + ";");

        topRow.getChildren().addAll(eyebrowLabel, versionBadge);

        messageLabel = new Label();
        messageLabel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + COLOR_NAVY + ";");
        messageLabel.setWrapText(true);

        textContainer.getChildren().addAll(topRow, messageLabel);

        // 3. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.NEVER);

        // 4. Action Buttons
        HBox actionsContainer = new HBox(8);
        actionsContainer.setAlignment(Pos.CENTER_RIGHT);

        updateBtn = new Button("ОНОВИТИ");
        updateBtn.setGraphic(createSVGIcon(ICON_UPDATE, Color.WHITE, 14));
        updateBtn.setOnAction(e -> onActionClicked());

        snoozeBtn = new Button("ВІДКЛАСТИ");
        String secondaryBtnStyle = DIALOG_SECONDARY_BUTTON_STYLE + "-fx-padding: 8 16; -fx-font-size: 12px; -fx-background-radius: 14; -fx-border-radius: 14;";
        snoozeBtn.setStyle(secondaryBtnStyle);
        snoozeBtn.setOnAction(e -> snoozeUpdate());

        closeBtn = new Button();
        closeBtn.setGraphic(createSVGIcon(ICON_DANGER, Color.web(COLOR_SLATE_LIGHT), 12));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6; -fx-background-radius: 99;");
        closeBtn.setTooltip(new Tooltip("Приховати це повідомлення"));
        closeBtn.setOnAction(e -> snoozeUpdate());

        actionsContainer.getChildren().addAll(updateBtn, snoozeBtn, closeBtn);

        getChildren().addAll(iconBadge, textContainer, actionsContainer);

        // Phase 2: React to background download state changes
        updateService.addStatusListener(status -> Platform.runLater(() -> updateStatusDisplay(status)));
        updateService.addProgressListener(progress -> Platform.runLater(() -> updateProgressDisplay(progress)));
    }

    public void show(UpdateManifest manifest) {
        if (manifest == null) return;
        this.currentManifest = manifest;

        // Apply visual theme according to state
        DownloadStatus status = updateService.getDownloadStatus();
        if (status == DownloadStatus.VERIFIED && manifest.latestVersion().equalsIgnoreCase(updateService.getDownloadingVersion())) {
            updateStatusDisplay(DownloadStatus.VERIFIED);
        } else if (status == DownloadStatus.DOWNLOADING && manifest.latestVersion().equalsIgnoreCase(updateService.getDownloadingVersion())) {
            updateStatusDisplay(DownloadStatus.DOWNLOADING);
        } else {
            applyManifestBaseTheme(manifest);
        }

        if (!isVisible()) {
            setVisible(true);
            setManaged(true);
            FadeTransition ft = new FadeTransition(Duration.millis(250), this);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    private void applyManifestBaseTheme(UpdateManifest manifest) {
        boolean isCritical = manifest.critical();

        if (isCritical) {
            setStyle(UPDATE_BANNER_CRITICAL_STYLE);
            iconBadge.setStyle("-fx-background-color: " + COLOR_DANGER_SOFT + "; -fx-border-color: " + COLOR_DANGER_BORDER + "; -fx-border-width: 1; -fx-background-radius: 14; -fx-border-radius: 14;");
            iconBadge.getChildren().setAll(createSVGIcon(ICON_ALERT, Color.web(COLOR_DANGER), 20));

            eyebrowLabel.setText("КРИТИЧНЕ ОНОВЛЕННЯ");
            eyebrowLabel.setStyle(PREMIUM_LABEL_STYLE + "-fx-text-fill: " + COLOR_DANGER + ";");

            versionBadge.setText("v" + manifest.latestVersion());
            versionBadge.setStyle("-fx-background-color: " + COLOR_DANGER_SOFT + "; -fx-background-radius: 99; -fx-padding: 1 8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-border-color: " + COLOR_DANGER_BORDER + "; -fx-border-radius: 99; -fx-text-fill: " + COLOR_DANGER + ";");

            messageLabel.setText("Виявлено критичне оновлення системи безпеки та стабільності. Рекомендується встановити його негайно.");

            String dangerBtnStyle = PREMIUM_BTN_SMALL_STYLE + "-fx-background-color: " + GRADIENT_DANGER + ";";
            updateBtn.setText("ОНОВИТИ");
            updateBtn.setStyle(dangerBtnStyle);
            updateBtn.setGraphic(createSVGIcon(ICON_UPDATE, Color.WHITE, 14));
            snoozeBtn.setText("ВІДКЛАСТИ (2 ГОД)");
        } else {
            setStyle(UPDATE_BANNER_STYLE);
            iconBadge.setStyle("-fx-background-color: " + COLOR_PURPLE_LIGHT + "; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-width: 1; -fx-background-radius: 14; -fx-border-radius: 14;");
            iconBadge.getChildren().setAll(createSVGIcon(ICON_UPDATE, Color.web(COLOR_INDIGO), 20));

            eyebrowLabel.setText("ДОСТУПНЕ ОНОВЛЕННЯ");
            eyebrowLabel.setStyle(PREMIUM_LABEL_STYLE + "-fx-text-fill: " + COLOR_INDIGO + ";");

            versionBadge.setText("v" + manifest.latestVersion());
            versionBadge.setStyle("-fx-background-color: white; -fx-background-radius: 99; -fx-padding: 1 8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 99; -fx-text-fill: " + COLOR_NAVY + ";");

            String dateStr = formatReleaseDate(manifest.releaseDate());
            messageLabel.setText(String.format("Доступна нова версія School Bell (%s). Натисніть «Оновити», щоб переглянути список змін та встановити.", dateStr));

            updateBtn.setText("ОНОВИТИ");
            updateBtn.setStyle(PREMIUM_BTN_SMALL_STYLE);
            updateBtn.setGraphic(createSVGIcon(ICON_UPDATE, Color.WHITE, 14));
            snoozeBtn.setText("ВІДКЛАСТИ (24 ГОД)");
        }
    }

    private void updateStatusDisplay(DownloadStatus status) {
        if (!isVisible() || currentManifest == null) return;

        if (status == DownloadStatus.DOWNLOADING) {
            updateProgressDisplay(updateService.getDownloadProgress());
            updateBtn.setText("ОНОВИТИ");
            updateBtn.setGraphic(createSVGIcon(ICON_UPDATE, Color.WHITE, 14));
        } else if (status == DownloadStatus.VERIFIED) {
            if (currentManifest.critical()) {
                setStyle(UPDATE_BANNER_CRITICAL_STYLE);
            } else {
                setStyle(UPDATE_BANNER_READY_STYLE);
            }
            iconBadge.setStyle("-fx-background-color: " + COLOR_SUCCESS_PALE + "; -fx-border-color: " + COLOR_SUCCESS_BORDER + "; -fx-border-width: 1; -fx-background-radius: 14; -fx-border-radius: 14;");
            iconBadge.getChildren().setAll(createSVGIcon(ICON_CHECK, Color.web(COLOR_SUCCESS), 20));

            eyebrowLabel.setText("ГОТОВО ДО ВСТАНОВЛЕННЯ");
            eyebrowLabel.setStyle(PREMIUM_LABEL_STYLE + "-fx-text-fill: " + COLOR_SUCCESS + ";");

            versionBadge.setText("v" + currentManifest.latestVersion());
            versionBadge.setStyle("-fx-background-color: " + COLOR_SUCCESS_PALE + "; -fx-background-radius: 99; -fx-padding: 1 8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-border-color: " + COLOR_SUCCESS_BORDER + "; -fx-border-radius: 99; -fx-text-fill: " + COLOR_SUCCESS + ";");

            messageLabel.setText(String.format("Оновлення v%s завантажено й готове до встановлення. Натисніть «Встановити», щоб оновити програму.", currentManifest.latestVersion()));

            updateBtn.setText("ВСТАНОВИТИ");
            updateBtn.setStyle(PREMIUM_BTN_SMALL_STYLE + "-fx-background-color: linear-gradient(to right, " + COLOR_SUCCESS + ", " + COLOR_TEAL_DARK + ");");
            updateBtn.setGraphic(createSVGIcon(ICON_CHECK, Color.WHITE, 14));
        } else if (status == DownloadStatus.FAILED) {
            messageLabel.setText(String.format("Доступна версія v%s, але фонове завантаження не вдалося. Натисніть для повторної спроби.", currentManifest.latestVersion()));
            updateBtn.setText("СПРОБУВАТИ ЗНОВУ");
            updateBtn.setGraphic(createSVGIcon(ICON_REFRESH, Color.WHITE, 14));
        }
    }

    private void updateProgressDisplay(double progress) {
        if (!isVisible() || currentManifest == null || updateService.getDownloadStatus() != DownloadStatus.DOWNLOADING) {
            return;
        }

        if (progress >= 0) {
            messageLabel.setText(String.format("Фонове завантаження версії v%s: %.0f%%...", currentManifest.latestVersion(), progress * 100));
        } else {
            messageLabel.setText(String.format("Фонове завантаження версії v%s: %.1f MB...", currentManifest.latestVersion(), -progress));
        }
    }

    public void dismissWithAnimation() {
        if (!isVisible()) return;

        FadeTransition ft = new FadeTransition(Duration.millis(200), this);
        ft.setFromValue(getOpacity());
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            setVisible(false);
            setManaged(false);
        });
        ft.play();
    }

    private void onActionClicked() {
        if (currentManifest == null) return;
        if (updateService.getDownloadStatus() == DownloadStatus.FAILED) {
            updateService.startBackgroundDownload(currentManifest);
            return;
        }
        openUpdateDialog();
    }

    private void openUpdateDialog() {
        if (currentManifest == null) return;
        UpdateAvailableDialog dialog = new UpdateAvailableDialog(mainApp.getStage(), updateService, currentManifest);
        dialog.setOnSnoozeCallback(this::dismissWithAnimation);
        dialog.display();
    }

    private void snoozeUpdate() {
        if (currentManifest == null) return;
        long duration = currentManifest.critical() ? (2 * 3600 * 1000L) : (24 * 3600 * 1000L);
        updateService.snoozeUpdate(currentManifest, duration);
        dismissWithAnimation();
    }

    private static String formatReleaseDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "Свіжий реліз";
        try {
            LocalDate date = LocalDate.parse(isoDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return "реліз від " + date.format(formatter);
        } catch (Exception e) {
            return "реліз від " + isoDate;
        }
    }
}
