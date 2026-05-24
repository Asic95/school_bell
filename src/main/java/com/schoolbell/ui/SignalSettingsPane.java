package com.schoolbell.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;

import java.net.URL;
import java.util.List;

import static com.schoolbell.ui.UIStyles.*;

public class SignalSettingsPane extends StackPane {
    private final IntegerProperty regularDuration = new SimpleIntegerProperty();
    private final IntegerProperty airRaidRingDuration = new SimpleIntegerProperty();
    private final IntegerProperty airRaidPauseDuration = new SimpleIntegerProperty();
    private final IntegerProperty emergencyDuration = new SimpleIntegerProperty();

    public SignalSettingsPane() {
        getStyleClass().add("bell-settings-root");
        applyStylesheet();

        VBox container = new VBox(28);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setFillWidth(true);
        container.getStyleClass().add("bell-settings-content-only");
        
        container.getChildren().addAll(buildRegularCard(), buildAirRaidCard(), buildEmergencyCard());

        getChildren().add(container);
    }

    public SignalSettingsPane(int regular, int airRing, int airPause, int emergency) {
        this();
        regularDuration.set(regular);
        airRaidRingDuration.set(airRing);
        airRaidPauseDuration.set(airPause);
        emergencyDuration.set(emergency);
    }

    public int getRegularDuration() {
        return regularDuration.get();
    }

    public int getAirRaidRingDuration() {
        return airRaidRingDuration.get();
    }

    public int getAirRaidPauseDuration() {
        return airRaidPauseDuration.get();
    }

    public int getEmergencyDuration() {
        return emergencyDuration.get();
    }

    private void applyStylesheet() {
        URL css = SignalSettingsPane.class.getResource("signal-settings.css");
        if (css != null) {
            getStylesheets().add(css.toExternalForm());
        }
    }

    private HBox buildRegularCard() {
        DurationStepper duration = new DurationStepper(5, 1, 30);
        duration.getStyleClass().add("stepper-regular");
        duration.valueProperty().bindBidirectional(regularDuration);

        WaveformCanvas waveform = new WaveformCanvas(WaveType.REGULAR, Color.web("#4A76FF"), duration.valueProperty(), null);
        HBox card = createCard(
                "Звичайний дзвінок",
                "Один безперервний сигнал",
                "M12,2A2,2 0 0,0 10,4A2,2 0 0,0 10,4.29C7.12,5.14 5,7.82 5,11V17L3,19V20H21V19L19,17V11C19,7.82 16.88,5.14 14,4.29C14,4.19 14,4.1 14,4A2,2 0 0,0 12,2M10,21A2,2 0 0,0 12,23A2,2 0 0,0 14,21H10Z",
                "tone-regular",
                List.of(labeledControl("Тривалість сигналу", duration)),
                waveform,
                "ВІЗУАЛІЗАЦІЯ СИГНАЛУ",
                "tone-text-regular"
        );
        return card;
    }

    private HBox buildAirRaidCard() {
        DurationStepper ring = new DurationStepper(3, 1, 30);
        DurationStepper pause = new DurationStepper(1, 1, 15);
        ring.getStyleClass().add("stepper-air");
        pause.getStyleClass().add("stepper-air");
        ring.valueProperty().bindBidirectional(airRaidRingDuration);
        pause.valueProperty().bindBidirectional(airRaidPauseDuration);

        HBox controlsRow = new HBox(12,
                labeledControl("Тривалість сигналу", ring),
                labeledControl("Тривалість паузи", pause)
        );
        controlsRow.getStyleClass().add("multi-control-row");

        WaveformCanvas waveform = new WaveformCanvas(WaveType.AIR_RAID, Color.web("#FF9D3F"), ring.valueProperty(), pause.valueProperty());
        HBox card = createCard(
                "Повітряна тривога",
                "Три коротких сигнали з паузами",
                "M12,2C9.79,2 8,3.79 8,6V10H16V6C16,3.79 14.21,2 12,2M4,12V14H20V12H4M6,14L4,22H20L18,14H6M2,8V10H6V8H2M18,8V10H22V8H18",
                "tone-air",
                List.of(controlsRow),
                waveform,
                "ВІЗУАЛІЗАЦІЯ СИГНАЛУ",
                "tone-text-air"
        );
        return card;
    }

    private HBox buildEmergencyCard() {
        DurationStepper duration = new DurationStepper(15, 5, 60);
        duration.getStyleClass().add("stepper-emergency");
        duration.valueProperty().bindBidirectional(emergencyDuration);

        WaveformCanvas waveform = new WaveformCanvas(WaveType.EMERGENCY, Color.web("#FF5F5F"), duration.valueProperty(), null);
        HBox card = createCard(
                "Екстрена ситуація",
                "Один тривалий безперервний сигнал",
                "M3,9V15H7L12,20V4L7,9H3M16.5,12C16.5,10.23 15.5,8.71 14,7.97V16.03C15.5,15.29 16.5,13.77 16.5,12M14,3.23V5.29C16.89,6.15 19,8.83 19,12C19,15.17 16.89,17.85 14,18.71V20.77C18,19.86 21,16.28 21,12C21,7.72 18,4.14 14,3.23Z",
                "tone-emergency",
                List.of(labeledControl("Тривалість сигналу", duration)),
                waveform,
                "ВІЗУАЛІЗАЦІЯ СИГНАЛУ",
                "tone-text-emergency"
        );
        return card;
    }

    private HBox createCard(
            String titleText,
            String subtitleText,
            String iconPath,
            String toneClass,
            List<Region> controls,
            WaveformCanvas waveform,
            String previewLabel,
            String previewToneClass
    ) {
        // Redesigned: Vertical stack card
        VBox card = new VBox(16);
        card.getStyleClass().add("bell-card");
        card.setMaxWidth(Double.MAX_VALUE);
        
        // Header Row: Icon + Title
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(0, 0, 8, 0)); // Add padding for vertical spacing
        StackPane iconWrap = new StackPane(icon(iconPath, "card-icon"));
        iconWrap.getStyleClass().addAll("card-icon-wrap", toneClass);
        iconWrap.setPrefSize(40, 40);
        
        VBox titleBlock = new VBox(2);
        Label labelTag = new Label("СИГНАЛ");
        labelTag.setStyle("-fx-font-size: 10px; -fx-text-fill: #a1a1aa; -fx-font-weight: 600;");
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        titleBlock.getChildren().addAll(labelTag, title);
        headerRow.getChildren().addAll(iconWrap, titleBlock);

        // Body Row: Configuration with alignment to match the title start
        VBox controlsWrapper = new VBox(16);
        controlsWrapper.setPadding(new Insets(0, 0, 0, 52));
        for (Region control : controls) {
            controlsWrapper.getChildren().add(control);
        }

        // Apply a fixed left margin to the waveform to align with the text block
        VBox waveformWrapper = new VBox(waveform);
        waveformWrapper.setPadding(new Insets(0, 0, 0, 52)); // 40 (icon) + 12 (spacing)

        card.getChildren().addAll(headerRow, controlsWrapper, waveformWrapper);
        
        return new HBox(card);
    }

    private VBox labeledControl(String labelText, DurationStepper control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("control-label");
        VBox box = new VBox(6, label, control);
        box.getStyleClass().add("labeled-control");
        return box;
    }

    private SVGPath icon(String content, String styleClass) {
        SVGPath icon = new SVGPath();
        icon.setContent(content);
        icon.getStyleClass().add(styleClass);
        return icon;
    }

    private class DurationStepper extends HBox {
        private final IntegerProperty value = new SimpleIntegerProperty();

        DurationStepper(int initial, int min, int max) {
            getStyleClass().add("duration-stepper");
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(8);

            Button minus = new Button();
            minus.getStyleClass().add("step-button");
            setPlusMinusIcon(minus, "M19,13H5V11H19V13Z");

            Button plus = new Button();
            plus.getStyleClass().add("step-button");
            setPlusMinusIcon(plus, "M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z");

            Label valueLabel = new Label();
            valueLabel.getStyleClass().add("step-value");
            value.set(initial);
            valueLabel.textProperty().bind(value.asString("%d сек"));
            valueLabel.setTextAlignment(TextAlignment.CENTER);

            minus.setOnAction(e -> value.set(Math.max(min, value.get() - 1)));
            plus.setOnAction(e -> value.set(Math.min(max, value.get() + 1)));
            getChildren().addAll(minus, valueLabel, plus);
        }

        IntegerProperty valueProperty() {
            return value;
        }
    }

    private void setPlusMinusIcon(Button button, String path) {
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.getStyleClass().add("step-icon");
        p.setScaleX(0.56);
        p.setScaleY(0.56);
        button.setGraphic(p);
    }

    private enum WaveType {REGULAR, AIR_RAID, EMERGENCY}

    private class WaveformCanvas extends Pane {
        private final Canvas canvas = new Canvas();
        private final WaveType type;
        private final Color color;
        private final IntegerProperty first;
        private final IntegerProperty second;
        private final Label sound1 = new Label();
        private final Label pause1 = new Label();
        private final Label sound2 = new Label();
        private final Label pause2 = new Label();
        private final Label sound3 = new Label();
        private final Line divider1 = new Line();
        private final Line divider2 = new Line();

        WaveformCanvas(WaveType type, Color color, IntegerProperty first, IntegerProperty second) {
            this.type = type;
            this.color = color;
            this.first = first;
            this.second = second;
            getStyleClass().add("wave-area");
            getChildren().add(canvas);
            setMinHeight(130);
            setPrefHeight(145);

            if (type == WaveType.AIR_RAID) {
                sound1.getStyleClass().addAll("wave-badge", "wave-badge-sound");
                pause1.getStyleClass().addAll("wave-badge", "wave-badge-pause");
                sound2.getStyleClass().addAll("wave-badge", "wave-badge-sound");
                pause2.getStyleClass().addAll("wave-badge", "wave-badge-pause");
                sound3.getStyleClass().addAll("wave-badge", "wave-badge-sound");
                divider1.getStyleClass().add("pause-divider");
                divider2.getStyleClass().add("pause-divider");
                getChildren().addAll(divider1, divider2, sound1, pause1, sound2, pause2, sound3);
            }

            widthProperty().addListener((obs, o, n) -> draw());
            heightProperty().addListener((obs, o, n) -> draw());
            first.addListener((obs, o, n) -> draw());
            if (second != null) second.addListener((obs, o, n) -> draw());
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            canvas.setWidth(getWidth());
            canvas.setHeight(getHeight());
            draw();
        }

        private void draw() {
            double w = Math.max(10, getWidth());
            double h = Math.max(10, getHeight());
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, w, h);

            double chartTop = 26;
            double chartBottom = h - 24;
            double chartHeight = chartBottom - chartTop;

            switch (type) {
                case REGULAR -> drawContinuous(gc, w, chartTop, chartHeight, 6.0, 4.0, first.get(), false);
                case AIR_RAID -> drawAirRaid(gc, w, chartTop, chartHeight, 6.0, 4.0, first.get(), second.get());
                case EMERGENCY -> drawContinuous(gc, w, chartTop, chartHeight, 7.0, 3.0, first.get(), true);
            }
        }

        private void drawContinuous(GraphicsContext gc, double w, double top, double h, double barW, double gap, int duration, boolean dense) {
            int bars = Math.max(20, (int) (w / (barW + gap)));
            gc.setFill(color.deriveColor(0, 1, 1, 0.9));
            for (int i = 0; i < bars; i++) {
                double base = dense ? 0.56 : 0.52;
                double spread = dense ? 0.36 : 0.28;
                double amp = Math.min(0.95, base + noise(i, dense ? 2.7 : 1.3) * spread);
                double height = h * amp;
                double x = i * (barW + gap) + 6;
                double y = top + (h - height) / 2.0; // Center vertically
                gc.fillRoundRect(x, y, barW, height, 4, 4);
            }
            drawTimeline(gc, w, top + h + 12, buildTimelineLabels(duration));
        }

        private void drawAirRaid(GraphicsContext gc, double w, double top, double h, double barW, double gap, int soundSec, int pauseSec) {
            int total = Math.max(1, soundSec * 3 + pauseSec * 2);
            double pxPerSec = (w - 12) / total;
            double x = 6;
            gc.setFill(color.deriveColor(0, 1, 1, 0.92));

            for (int section = 0; section < 3; section++) {
                double secWidth = soundSec * pxPerSec;
                int bars = Math.max(4, (int) (secWidth / (barW + gap)));
                for (int i = 0; i < bars; i++) {
                    double amp = 0.55 + noise(i + section * 37, 1.7) * 0.28;
                    double bh = h * Math.min(0.95, amp);
                    gc.fillRoundRect(x + i * (barW + gap), top + (h - bh) / 2.0, barW, bh, 4, 4);
                }

                Label s = section == 0 ? sound1 : (section == 1 ? sound2 : sound3);
                s.setText("звук " + soundSec + "с");
                s.setLayoutX(x + secWidth * 0.35);
                s.setLayoutY(8);
                x += secWidth;

                if (section < 2) {
                    double pauseWidth = pauseSec * pxPerSec;
                    Line d = section == 0 ? divider1 : divider2;
                    d.setStartX(x + pauseWidth / 2.0);
                    d.setEndX(x + pauseWidth / 2.0);
                    d.setStartY(top + 2);
                    d.setEndY(top + h);
                    Label p = section == 0 ? pause1 : pause2;
                    p.setText("пауза " + pauseSec + "с");
                    p.setLayoutX(x + pauseWidth * 0.18);
                    p.setLayoutY(8);
                    x += pauseWidth;
                }
            }
        }

        private void drawTimeline(GraphicsContext gc, double w, double y, String[] labels) {
            gc.setFill(Color.web("#8B97A8"));
            gc.setFont(javafx.scene.text.Font.font("Inter", 11));
            double leftPad = 6;
            double rightPad = 6;
            double span = Math.max(1, w - leftPad - rightPad);
            double step = labels.length > 1 ? span / (labels.length - 1) : 0;
            for (int i = 0; i < labels.length; i++) {
                double x = leftPad + i * step;
                if (i == 0) {
                    gc.setTextAlign(TextAlignment.LEFT);
                } else if (i == labels.length - 1) {
                    gc.setTextAlign(TextAlignment.RIGHT);
                } else {
                    gc.setTextAlign(TextAlignment.CENTER);
                }
                gc.fillText(labels[i], x, y);
            }
            gc.setTextAlign(TextAlignment.LEFT);
        }

        private String[] buildTimelineLabels(int durationSec) {
            int duration = Math.max(1, durationSec);
            int points = duration <= 6 ? duration + 1 : 5;
            if (points < 2) {
                points = 2;
            }
            String[] labels = new String[points];
            for (int i = 0; i < points; i++) {
                int value = (int) Math.round((double) duration * i / (points - 1));
                labels[i] = value + "с";
            }
            return labels;
        }

        private double normalized(double x) {
            return (Math.sin(x) + 1.0) * 0.5;
        }

        private double noise(int i, double seed) {
            double a = normalized(i * 0.73 + seed);
            double b = normalized(i * 1.91 + seed * 2.3);
            double c = normalized(i * 2.47 + seed * 0.9);
            return Math.min(1.0, (a * 0.5) + (b * 0.35) + (c * 0.15));
        }
    }
}
