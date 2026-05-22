package com.schoolbell.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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

public class BellSettingsPane extends StackPane {
    private final IntegerProperty regularDuration = new SimpleIntegerProperty();
    private final IntegerProperty airRaidRingDuration = new SimpleIntegerProperty();
    private final IntegerProperty airRaidPauseDuration = new SimpleIntegerProperty();
    private final IntegerProperty emergencyDuration = new SimpleIntegerProperty();
    private final IntegerProperty earlyMin = new SimpleIntegerProperty();
    private final IntegerProperty earlySec = new SimpleIntegerProperty();

    public BellSettingsPane() {
        this(false);
    }

    public BellSettingsPane(boolean embedded) {
        getStyleClass().add("bell-settings-root");
        if (embedded) {
            getStyleClass().add("bell-settings-embedded");
        }
        applyStylesheet();

        VBox container = new VBox(28);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setFillWidth(true);

        if (!embedded) {
            container.getStyleClass().add("bell-settings-container");
            container.getChildren().add(ControlFactory.createPageHeader(
                "НАЛАШТУВАННЯ",
                "Розклад та дзвінки",
                "Конфігурація тривалості сигналів та режимів роботи шкільної системи.",
                ICON_CLOCK,
                "#4f46e5",
                null
            ));
        } else {
            container.getStyleClass().add("bell-settings-content-only");
        }

        container.getChildren().addAll(buildRegularCard(), buildAirRaidCard(), buildEmergencyCard());

        if (!embedded) {
            VBox centered = new VBox(container);
            centered.setAlignment(Pos.TOP_CENTER);
            centered.setPadding(new Insets(26, 32, 26, 32));
            getChildren().add(centered);
        } else {
            getChildren().add(container);
        }
    }

    public BellSettingsPane(int regular, int airRing, int airPause, int emergency, int earlyM, int earlyS, boolean embedded) {
        this(embedded);
        regularDuration.set(regular);
        airRaidRingDuration.set(airRing);
        airRaidPauseDuration.set(airPause);
        emergencyDuration.set(emergency);
        earlyMin.set(earlyM);
        earlySec.set(earlyS);
    }

    public int getRegularDuration() { return regularDuration.get(); }
    public int getAirRaidRingDuration() { return airRaidRingDuration.get(); }
    public int getAirRaidPauseDuration() { return airRaidPauseDuration.get(); }
    public int getEmergencyDuration() { return emergencyDuration.get(); }
    public int getEarlyMin() { return earlyMin.get(); }
    public int getEarlySec() { return earlySec.get(); }

    private void applyStylesheet() {
        URL css = BellSettingsPane.class.getResource("bell-settings.css");
        if (css != null) {
            getStylesheets().add(css.toExternalForm());
        }
    }

    private HBox buildRegularCard() {
        DurationStepper duration = new DurationStepper(5, 1, 30, "сек");
        duration.getStyleClass().add("stepper-regular");
        duration.valueProperty().bindBidirectional(regularDuration);

        DurationStepper eMin = new DurationStepper(0, 0, 10, "хв");
        DurationStepper eSec = new DurationStepper(0, 0, 59, "сек");
        eMin.getStyleClass().add("stepper-regular");
        eSec.getStyleClass().add("stepper-regular");
        eMin.valueProperty().bindBidirectional(earlyMin);
        eSec.valueProperty().bindBidirectional(earlySec);

        VBox verticalControls = new VBox(16,
                labeledControl("Тривалість основного сигналу", duration),
                labeledControl("Завчасне сповіщення перед основним дзвінком", new HBox(8, eMin, eSec))
        );

        WaveformCanvas waveform = new WaveformCanvas(WaveType.REGULAR, Color.web("#4A76FF"), regularDuration, earlyMin, earlySec);
        HBox card = createCard(
                "Звичайний дзвінок",
                "Сигнали уроків та завчасне сповіщення",
                "M12,2A2,2 0 0,0 10,4A2,2 0 0,0 10,4.29C7.12,5.14 5,7.82 5,11V17L3,19V20H21V19L19,17V11C19,7.82 16.88,5.14 14,4.29C14,4.19 14,4.1 14,4A2,2 0 0,0 12,2M10,21A2,2 0 0,0 12,23A2,2 0 0,0 14,21H10Z",
                "tone-regular",
                List.of(verticalControls),
                waveform,
                "ВІЗУАЛІЗАЦІЯ СИГНАЛУ",
                "tone-text-regular"
        );
        return card;
    }

    private HBox buildAirRaidCard() {
        DurationStepper ring = new DurationStepper(3, 1, 30, "сек");
        DurationStepper pause = new DurationStepper(1, 1, 15, "сек");
        ring.getStyleClass().add("stepper-air");
        pause.getStyleClass().add("stepper-air");
        ring.valueProperty().bindBidirectional(airRaidRingDuration);
        pause.valueProperty().bindBidirectional(airRaidPauseDuration);

        HBox controlsRow = new HBox(12,
                labeledControl("Тривалість сигналу", ring),
                labeledControl("Тривалість паузи", pause)
        );
        controlsRow.getStyleClass().add("multi-control-row");

        WaveformCanvas waveform = new WaveformCanvas(WaveType.AIR_RAID, Color.web("#FF9D3F"), ring.valueProperty(), pause.valueProperty(), null);
        HBox card = createCard(
                "Повітряна тривога",
                "Три коротких сигнали з паузами",
                ICON_AIR_RAID,
                "tone-air",
                List.of(controlsRow),
                waveform,
                "ВІЗУАЛІЗАЦІЯ СИГНАЛУ",
                "tone-text-air"
        );
        return card;
    }

    private HBox buildEmergencyCard() {
        DurationStepper duration = new DurationStepper(15, 5, 60, "сек");
        duration.getStyleClass().add("stepper-emergency");
        duration.valueProperty().bindBidirectional(emergencyDuration);

        WaveformCanvas waveform = new WaveformCanvas(WaveType.EMERGENCY, Color.web("#FF5F5F"), duration.valueProperty(), null, null);
        HBox card = createCard(
                "Екстрена ситуація",
                "Один тривалий безперервний сигнал",
                ICON_LIFEBUOY,
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
        HBox card = new HBox(32);
        card.getStyleClass().add("bell-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(24));
        
        VBox leftBlock = new VBox(12);
        leftBlock.setPrefWidth(350);
        leftBlock.setMinWidth(350);
        leftBlock.setMaxWidth(350);
        
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        StackPane iconWrap = new StackPane(icon(iconPath, "card-icon"));
        iconWrap.getStyleClass().addAll("card-icon-wrap", toneClass);
        iconWrap.setPrefSize(48, 48);
        
        VBox titleBlock = new VBox(2);
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #2d3436;");
        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #636e72;");
        titleBlock.getChildren().addAll(title, subtitle);
        headerRow.getChildren().addAll(iconWrap, titleBlock);

        HBox controlsRow = new HBox(16);
        controlsRow.setAlignment(Pos.CENTER_LEFT);
        controlsRow.setPadding(new Insets(0, 0, 0, 60));
        controlsRow.getChildren().addAll(controls);

        leftBlock.getChildren().addAll(headerRow, controlsRow);

        VBox rightBlock = new VBox(8);
        rightBlock.getChildren().addAll(waveform);
        HBox.setHgrow(rightBlock, Priority.ALWAYS);

        card.getChildren().addAll(leftBlock, rightBlock);
        return card;
    }

    private VBox labeledControl(String labelText, Node control) {
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

        DurationStepper(int initial, int min, int max, String unit) {
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
            valueLabel.textProperty().bind(value.asString("%d " + unit));
            valueLabel.setTextAlignment(TextAlignment.CENTER);

            minus.setOnAction(e -> value.set(Math.max(min, value.get() - 1)));
            plus.setOnAction(e -> value.set(Math.min(max, value.get() + 1)));
            getChildren().addAll(minus, valueLabel, plus);
        }

        IntegerProperty valueProperty() { return value; }
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
        private static final double START_X = 12.0;
        private final Canvas canvas = new Canvas();
        private final WaveType type;
        private final Color color;
        private final IntegerProperty mainDur;
        private final IntegerProperty extra1; // air: pause, regular: earlyMin
        private final IntegerProperty extra2; // regular: earlySec
        
        private final Label label1 = new Label();
        private final Label labelPause = new Label();
        private final Label label2 = new Label();
        private final Label labelPause2 = new Label();
        private final Label label3 = new Label();
        private final Line divider1 = new Line();
        private final Line divider2 = new Line();

        WaveformCanvas(WaveType type, Color color, IntegerProperty mainDur, IntegerProperty extra1, IntegerProperty extra2) {
            this.type = type;
            this.color = color;
            this.mainDur = mainDur;
            this.extra1 = extra1;
            this.extra2 = extra2;
            getStyleClass().add("wave-area");
            getChildren().add(canvas);
            setMinHeight(130);
            setPrefHeight(145);

            if (type == WaveType.AIR_RAID || type == WaveType.REGULAR) {
                for (Label l : List.of(label1, labelPause, label2, labelPause2, label3)) {
                    l.getStyleClass().add("wave-badge");
                }
                
                if (type == WaveType.REGULAR) {
                    label1.getStyleClass().add("wave-badge-regular-sound");
                    label2.getStyleClass().add("wave-badge-regular-sound");
                    label3.getStyleClass().add("wave-badge-regular-sound");
                    labelPause.getStyleClass().add("wave-badge-regular-pause");
                    labelPause2.getStyleClass().add("wave-badge-regular-pause");
                } else {
                    label1.getStyleClass().add("wave-badge-sound");
                    label2.getStyleClass().add("wave-badge-sound");
                    label3.getStyleClass().add("wave-badge-sound");
                    labelPause.getStyleClass().add("wave-badge-pause");
                    labelPause2.getStyleClass().add("wave-badge-pause");
                }
                
                divider1.getStyleClass().add("pause-divider");
                divider2.getStyleClass().add("pause-divider");
                
                getChildren().addAll(divider1, divider2, label1, labelPause, label2, labelPause2, label3);
            }

            widthProperty().addListener((obs, o, n) -> draw());
            heightProperty().addListener((obs, o, n) -> draw());
            mainDur.addListener((obs, o, n) -> draw());
            if (extra1 != null) extra1.addListener((obs, o, n) -> draw());
            if (extra2 != null) extra2.addListener((obs, o, n) -> draw());
        }

        @Override protected void layoutChildren() {
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

            label1.setVisible(false); labelPause.setVisible(false); label2.setVisible(false);
            labelPause2.setVisible(false); label3.setVisible(false);
            divider1.setVisible(false); divider2.setVisible(false);

            switch (type) {
                case REGULAR -> drawRegular(gc, w, chartTop, chartHeight);
                case AIR_RAID -> drawAirRaid(gc, w, chartTop, chartHeight);
                case EMERGENCY -> drawContinuous(gc, w, chartTop, chartHeight, 7.0, 3.0, mainDur.get(), true);
            }
        }

        private void drawRegular(GraphicsContext gc, double w, double top, double h) {
            int eMin = extra1.get();
            int eSec = extra2.get();
            int dur = mainDur.get();
            boolean hasEarly = (eMin > 0 || eSec > 0);

            if (!hasEarly) {
                drawContinuous(gc, w, top, h, 6.0, 4.0, dur, false);
                return;
            }

            label1.setVisible(true); labelPause.setVisible(true); label2.setVisible(true);
            divider1.setVisible(true);

            double barW = 6.0; double gap = 4.0;
            double pauseW = 120.0; // Fixed symbolic pause width
            double bellW = (w - START_X - 10 - pauseW) / 2.0;
            bellW = Math.max(60, bellW);

            gc.setFill(color.deriveColor(0, 1, 1, 0.9));
            
            // 1. Early Bell (First Section)
            int bars1 = (int)(bellW / (barW+gap));
            for (int i = 0; i < bars1; i++) {
                double bh = h * (0.52 + noise(i, 1.3) * 0.28);
                gc.fillRoundRect(START_X + i*(barW+gap), top+h-bh, barW, bh, 4, 4);
            }
            label1.setText("дзвінок " + dur + "с");
            label1.setLayoutX(START_X + (bellW - 60)/2.0); label1.setLayoutY(8);

            // 2. Pause (Center Section)
            double pauseStartX = START_X + bellW;
            divider1.setStartX(pauseStartX + pauseW/2.0);
            divider1.setEndX(pauseStartX + pauseW/2.0);
            divider1.setStartY(top+2); divider1.setEndY(top+h);
            
            labelPause.setText("пауза " + (eMin > 0 ? eMin + "хв " : "") + (eSec > 0 ? eSec + "с" : ""));
            // Center pause label over the gap
            labelPause.setLayoutX(pauseStartX + (pauseW - 80)/2.0); labelPause.setLayoutY(8);

            // 3. Main Bell (Final Section)
            double bell2StartX = pauseStartX + pauseW;
            int bars2 = (int)(bellW / (barW+gap));
            for (int i = 0; i < bars2; i++) {
                double bh = h * (0.52 + noise(i+100, 1.3) * 0.28);
                gc.fillRoundRect(bell2StartX + i*(barW+gap), top+h-bh, barW, bh, 4, 4);
            }
            label2.setText("дзвінок " + dur + "с");
            label2.setLayoutX(bell2StartX + (bellW - 60)/2.0); label2.setLayoutY(8);

            drawTimeline(gc, w, top + h + 12, new String[]{"-" + (eMin > 0 ? eMin + "хв " : "") + eSec + "с", "ПОЧАТОК УРОКУ"});
        }

        private void drawContinuous(GraphicsContext gc, double w, double top, double h, double barW, double gap, int duration, boolean dense) {
            int bars = Math.max(20, (int) ((w - START_X - 10) / (barW + gap)));
            gc.setFill(color.deriveColor(0, 1, 1, 0.9));
            for (int i = 0; i < bars; i++) {
                double base = dense ? 0.56 : 0.52;
                double spread = dense ? 0.36 : 0.28;
                double amp = Math.min(0.95, base + noise(i, dense ? 2.7 : 1.3) * spread);
                double height = h * amp;
                double x = START_X + i * (barW + gap);
                double y = top + h - height;
                gc.fillRoundRect(x, y, barW, height, 4, 4);
            }
            drawTimeline(gc, w, top + h + 12, buildTimelineLabels(duration));
        }

        private void drawAirRaid(GraphicsContext gc, double w, double top, double h) {
            int soundSec = mainDur.get();
            int pauseSec = extra1.get();
            label1.setVisible(true); labelPause.setVisible(true); label2.setVisible(true);
            labelPause2.setVisible(true); label3.setVisible(true);
            divider1.setVisible(true); divider2.setVisible(true);

            int total = Math.max(1, soundSec * 3 + pauseSec * 2);
            double availableWidth = w - START_X - 10;
            double pxPerSec = availableWidth / total;
            double x = START_X;
            double barW = 6.0; double gap = 4.0;
            gc.setFill(color.deriveColor(0, 1, 1, 0.92));

            for (int section = 0; section < 3; section++) {
                double secWidth = soundSec * pxPerSec;
                int bars = Math.max(4, (int) (secWidth / (barW + gap)));
                for (int i = 0; i < bars; i++) {
                    double amp = 0.55 + noise(i + section * 37, 1.7) * 0.28;
                    double bh = h * Math.min(0.95, amp);
                    gc.fillRoundRect(x + i * (barW + gap), top + h - bh, barW, bh, 4, 4);
                }
                Label s = section == 0 ? label1 : (section == 1 ? label2 : label3);
                s.setText("дзвінок " + soundSec + "с");
                s.setLayoutX(x + (secWidth - 60) / 2.0); s.setLayoutY(8);
                x += secWidth;
                if (section < 2) {
                    double pauseWidth = pauseSec * pxPerSec;
                    Line d = section == 0 ? divider1 : divider2;
                    d.setStartX(x + pauseWidth / 2.0); d.setEndX(x + pauseWidth / 2.0);
                    d.setStartY(top + 2); d.setEndY(top + h);
                    Label p = section == 0 ? labelPause : labelPause2;
                    p.setText("пауза " + pauseSec + "с");
                    p.setLayoutX(x + (pauseWidth - 70) / 2.0); p.setLayoutY(8);
                    x += pauseWidth;
                }
            }
            drawTimeline(gc, w, top + h + 12, buildTimelineLabels(total));
        }

        private void drawTimeline(GraphicsContext gc, double w, double y, String[] labels) {
            gc.setFill(Color.web("#8B97A8"));
            gc.setFont(javafx.scene.text.Font.font("Inter", 11));
            double leftPad = START_X; double span = Math.max(1, w - leftPad - 10);
            double step = labels.length > 1 ? span / (labels.length - 1) : 0;
            for (int i = 0; i < labels.length; i++) {
                double x = leftPad + i * step;
                gc.setTextAlign(i == 0 ? TextAlignment.LEFT : (i == labels.length - 1 ? TextAlignment.RIGHT : TextAlignment.CENTER));
                gc.fillText(labels[i], x, y);
            }
        }

        private String[] buildTimelineLabels(int durationSec) {
            int duration = Math.max(1, durationSec);
            int points = duration <= 6 ? duration + 1 : 5;
            String[] labels = new String[points];
            for (int i = 0; i < points; i++) {
                labels[i] = (int) Math.round((double) duration * i / (points - 1)) + "с";
            }
            return labels;
        }

        private double noise(int i, double seed) {
            double a = (Math.sin(i * 0.73 + seed) + 1.0) * 0.5;
            double b = (Math.sin(i * 1.91 + seed * 2.3) + 1.0) * 0.5;
            return (a * 0.6 + b * 0.4);
        }
    }
}
