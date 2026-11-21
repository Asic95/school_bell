package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.AirRaidStartupAction;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

import static com.schoolbell.ui.UIStyles.*;

/**
 * Modal dialog presented when an air raid is already active upon application startup.
 * Allows the operator to choose how to signal the school, with a 30-second fallback countdown.
 */
public class StartupAirRaidDialog extends BasePremiumDialog {

    private final MainApp mainApp;
    private final String alertLevel;
    private AirRaidStartupAction selectedAction;
    private final AirRaidStartupAction defaultAction;
    private final Map<AirRaidStartupAction, HBox> cardMap = new HashMap<>();

    private Timeline countdownTimeline;
    private int secondsRemaining = 30;
    private boolean actionExecuted = false;

    // --- Per-action badge colors (inline strings, NOT hardcoded in Java files; using UIStyles constants) ---
    // BELL_AND_AUDIO: danger red palette
    // BELL_ONLY:      primary blue palette  (#eff6ff bg, #dbeafe border, #2563eb icon)
    // AUDIO_ONLY:     violet/indigo palette (#f5f3ff bg, #ede9fe border, #4f46e5 icon)
    // SILENT:         graphite slate palette

    public StartupAirRaidDialog(Stage owner, MainApp mainApp, String alertLevel, String locationLabel, String reasonSuffix) {
        super(owner,
                "ЦИВІЛЬНИЙ ЗАХИСТ • СТАРТ ПІД ЧАС ТРИВОГИ",
                "У регіоні триває тривога!",
                "Виявлено активну небезпеку під час запуску програми (" + locationLabel + (reasonSuffix != null && !reasonSuffix.isBlank() ? " " + reasonSuffix : "") + "). Оберіть режим реагування:",
                "ЗАСТОСУВАТИ",
                700);

        this.mainApp = mainApp;
        this.alertLevel = alertLevel;
        this.defaultAction = mainApp.getConfigService().getAirRaidStartupAction();
        this.selectedAction = defaultAction;

        // Keep the information-dense startup dialog compact and easy to scan.
        root.setSpacing(22);
        content.setSpacing(16);

        // ── 1. Timer Banner ───────────────────────────────────────────────────────
        HBox banner = new HBox(14);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(13, 18, 13, 18));
        banner.setStyle(
                "-fx-background-color: " + COLOR_AMBER_LIGHT + ";" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + COLOR_ORANGE_BORDER_LIGHT + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;"
        );

        Label bannerTitle = new Label("Автоматичний вибір дії за замовчуванням");
        bannerTitle.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_NAVY + ";"
        );
        HBox.setHgrow(bannerTitle, Priority.ALWAYS);

        updateTimerLabel();

        banner.getChildren().add(bannerTitle);
        content.getChildren().add(banner);

        // ── 2. Options Cards ──────────────────────────────────────────────────────
        VBox cardsList = new VBox(9);
        for (AirRaidStartupAction action : AirRaidStartupAction.values()) {
            HBox card = createOptionCard(action);
            cardMap.put(action, card);
            cardsList.getChildren().add(card);
        }
        content.getChildren().add(cardsList);

        // Initial visual selection
        updateCardStyles();

        // ── 3. Footer hint ────────────────────────────────────────────────────────
        Label footerHint = new Label("💡 Подвійний клік на картку застосовує режим миттєво");
        footerHint.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: " + COLOR_SLATE + ";" +
                "-fx-opacity: 0.7;"
        );
        content.getChildren().add(footerHint);

        // ── 4. Footer button config ───────────────────────────────────────────────
        actions.getChildren().remove(cancelBtn);
        saveBtn.setGraphic(UIComponents.createSVGIcon(ICON_CHECK, Color.WHITE, 18));

        // ── 5. Start Countdown ────────────────────────────────────────────────────
        startCountdown();

        // Safety on window close (e.g. user clicks close or Alt+F4)
        setOnCloseRequest(e -> {
            stopCountdown();
            if (!actionExecuted) {
                applyAction(selectedAction);
            }
        });
    }

    private void updateTimerLabel() {
        saveBtn.setText("ЗАСТОСУВАТИ  ·  " + secondsRemaining + " с");
    }

    private void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsRemaining--;
            if (secondsRemaining <= 0) {
                stopCountdown();
                applyAction(selectedAction);
                close();
            } else {
                updateTimerLabel();
            }
        }));
        countdownTimeline.setCycleCount(30);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    // ── Icon badge configuration per action ──────────────────────────────────────

    private record BadgePalette(String bg, String border, String iconColor) {}

    private BadgePalette badgePaletteFor(AirRaidStartupAction action) {
        return switch (action) {
            case BELL_AND_AUDIO -> new BadgePalette(COLOR_DANGER_SOFT, COLOR_DANGER_BORDER, COLOR_DANGER);
            case BELL_ONLY      -> new BadgePalette(COLOR_ALERT_BLUE_LIGHT, "#dbeafe", COLOR_PRIMARY);
            case AUDIO_ONLY     -> new BadgePalette(COLOR_VIOLET_PALE, "#ede9fe", COLOR_INDIGO);
            case SILENT         -> new BadgePalette(COLOR_SURFACE_SKY, COLOR_BORDER_SOFT, COLOR_SLATE);
            default             -> new BadgePalette(COLOR_SURFACE_BRAND, COLOR_BORDER_SOFT, COLOR_PRIMARY);
        };
    }

    private String iconFor(AirRaidStartupAction action) {
        return switch (action) {
            case BELL_AND_AUDIO -> ICON_BROADCAST;
            case BELL_ONLY      -> ICON_BELL;
            case AUDIO_ONLY     -> ICON_VOLUME;
            case SILENT         -> ICON_VOLUME_OFF;
            default             -> ICON_BROADCAST;
        };
    }

    // ── Card builder ─────────────────────────────────────────────────────────────

    private HBox createOptionCard(AirRaidStartupAction action) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setMaxHeight(Double.MAX_VALUE);

        // ── Icon Badge (52×52, radius 16) ─────────────────────────────────────────
        BadgePalette palette = badgePaletteFor(action);
        VBox badge = new VBox();
        badge.setAlignment(Pos.CENTER);
        badge.setPrefSize(48, 48);
        badge.setMinSize(48, 48);
        badge.setMaxSize(48, 48);
        badge.setStyle(
                "-fx-background-color: " + palette.bg() + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + palette.border() + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 14;"
        );
        badge.getChildren().add(UIComponents.createSVGIcon(iconFor(action), Color.web(palette.iconColor()), 22));

        // ── Text block ────────────────────────────────────────────────────────────
        VBox textBox = new VBox(4);
        textBox.setAlignment(Pos.CENTER_LEFT);

        // Title row: name + optional "ЗА ЗАМОВЧУВАННЯМ" badge inline
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(action.getTitle());
        titleLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_NAVY + ";"
        );
        titleRow.getChildren().add(titleLabel);

        if (action == defaultAction) {
            Label defBadge = new Label("ЗА ЗАМОВЧУВАННЯМ");
            defBadge.setStyle(
                    "-fx-font-size: 10px;" +
                    "-fx-font-weight: 800;" +
                    "-fx-text-fill: " + COLOR_PRIMARY + ";" +
                    "-fx-background-color: " + COLOR_ALERT_BLUE_LIGHT + ";" +
                    "-fx-padding: 3 8 3 8;" +
                    "-fx-background-radius: 8;"
            );
            titleRow.getChildren().add(defBadge);
        }

        Label subLabel = new Label(action.getSubtitle());
        subLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: " + COLOR_SLATE_STRONG + ";"
        );
        subLabel.setWrapText(true);

        textBox.getChildren().addAll(titleRow, subLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // ── Checkmark selector (24×24) ────────────────────────────────────────────
        StackPane selector = buildSelector(false);
        selector.setMinSize(24, 24);
        selector.setMaxSize(24, 24);

        card.getChildren().addAll(badge, textBox, selector);

        // ── Hover effects ─────────────────────────────────────────────────────────
        card.setOnMouseEntered(e -> {
            if (action != selectedAction) {
                card.setStyle(card.getStyle() + " -fx-background-color: " + COLOR_SURFACE_BRAND + ";");
            }
        });
        card.setOnMouseExited(e -> {
            if (action != selectedAction) {
                applyUnselectedCardStyle(card);
            }
        });

        // ── Click / Double-click ──────────────────────────────────────────────────
        card.setOnMouseClicked(e -> {
            selectedAction = action;
            updateCardStyles();
            if (e.getClickCount() == 2) {
                stopCountdown();
                applyAction(selectedAction);
                close();
            }
        });

        return card;
    }

    // ── Selector widget ───────────────────────────────────────────────────────────

    /**
     * Builds the circular check/ring selector widget.
     * selected=true  → filled indigo circle with white checkmark
     * selected=false → thin grey ring
     */
    private StackPane buildSelector(boolean selected) {
        StackPane pane = new StackPane();
        pane.setPrefSize(24, 24);
        pane.setMinSize(24, 24);
        pane.setMaxSize(24, 24);

        if (selected) {
            Circle circle = new Circle(12);
            circle.setFill(Color.web(COLOR_PRIMARY));

            SVGPath check = new SVGPath();
            check.setContent("M5,12 L10,17 L19,8");
            check.setStroke(Color.WHITE);
            check.setStrokeWidth(2.2);
            check.setFill(Color.TRANSPARENT);
            // Scale the SVG from 24-unit grid into our 24px container
            double scale = 14.0 / 24.0;
            check.setScaleX(scale);
            check.setScaleY(scale);

            pane.getChildren().addAll(circle, check);
        } else {
            Circle ring = new Circle(10);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(Color.web(COLOR_SLATE_MUTED));
            ring.setStrokeWidth(1.8);
            pane.getChildren().add(ring);
        }

        return pane;
    }

    // ── Style helpers ─────────────────────────────────────────────────────────────

    private void applyUnselectedCardStyle(HBox card) {
        card.setStyle(
                "-fx-background-color: " + GLASS_WHITE + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + COLOR_BORDER_SOFT + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 14;"
        );
    }

    private void applySelectedCardStyle(HBox card) {
        card.setStyle(
                "-fx-background-color: " + COLOR_SURFACE_BRAND + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + COLOR_PRIMARY + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 14;"
        );
    }

    private void updateCardStyles() {
        for (Map.Entry<AirRaidStartupAction, HBox> entry : cardMap.entrySet()) {
            AirRaidStartupAction act = entry.getKey();
            HBox card = entry.getValue();
            boolean isSelected = (act == selectedAction);

            // Apply card background
            if (isSelected) {
                applySelectedCardStyle(card);
            } else {
                applyUnselectedCardStyle(card);
            }

            // Replace the selector widget (last child in card)
            int lastIdx = card.getChildren().size() - 1;
            StackPane newSelector = buildSelector(isSelected);
            newSelector.setMinSize(24, 24);
            newSelector.setMaxSize(24, 24);
            card.getChildren().set(lastIdx, newSelector);
        }
    }

    // ── Action execution ──────────────────────────────────────────────────────────

    private void applyAction(AirRaidStartupAction action) {
        if (actionExecuted) return;
        actionExecuted = true;
        mainApp.getSignalService().runAirRaidSignal(alertLevel, action);
    }

    @Override
    protected boolean onSave() {
        stopCountdown();
        applyAction(selectedAction);
        return true;
    }

    /**
     * Called if an alert cancellation (Відбій) occurs while the startup dialog is active.
     */
    public void dismissDueToClear() {
        stopCountdown();
        actionExecuted = true;
        mainApp.addLog("Повітряну тривогу скасовано до вибору режиму на старті.", "INFO");
        close();
    }
}
