package com.schoolbell.ui;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class ControlFactory {

    public static HBox createPageHeader(String eyebrow, String title, String subtitle, String icon, String iconColor, Node action) {
        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setFillHeight(false);

        VBox badge = new VBox(createSVGIcon(icon, Color.web(COLOR_PRIMARY), 24));
        badge.setAlignment(Pos.CENTER);
        badge.setPrefSize(54, 54);
        badge.setMinSize(54, 54);
        badge.setMaxSize(54, 54);
        VBox.setVgrow(badge, Priority.NEVER);
        HBox.setHgrow(badge, Priority.NEVER);
        badge.setStyle(ICON_BADGE_STYLE);

        VBox text = new VBox(2);
        Label eb = new Label(eyebrow.toUpperCase());
        eb.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");

        Label t = new Label(title);
        t.setStyle(DIALOG_TITLE_STYLE);

        Label s = new Label(subtitle);
        s.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: " + COLOR_SLATE + ";");
        s.setWrapText(true);

        text.getChildren().addAll(eb, t, s);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(badge, text, spacer);
        if (action != null) header.getChildren().add(action);

        return header;
    }

    public static Button createPrimaryActionButton(String text, String iconPath) {
        return createActionButton(text, iconPath, PREMIUM_BTN_STYLE, COLOR_INDIGO_DARK, COLOR_PRIMARY_DARK, SHADOW_INDIGO_40);
    }

    public static Button createDangerActionButton(String text, String iconPath) {
        String dangerStyle = PREMIUM_BTN_STYLE + "-fx-background-color: " + GRADIENT_DANGER + ";";
        return createActionButton(text, iconPath, dangerStyle, "#c0392b", "#a93226", SHADOW_BLACK_30);
    }

    public static Button createSmallPrimaryActionButton(String text, String iconPath) {
        return createActionButton(text, iconPath, PREMIUM_BTN_SMALL_STYLE, COLOR_INDIGO_DARK, COLOR_PRIMARY_DARK, SHADOW_INDIGO_40);
    }

    private static Button createActionButton(String text, String iconPath, String baseStyle, String hoverStart, String hoverEnd, String shadow) {
        Button btn = new Button(text);
        if (iconPath != null) {
            btn.setGraphic(createSVGIcon(iconPath, Color.WHITE, 18));
            btn.setGraphicTextGap(10);
        }

        btn.setStyle(baseStyle);

        // Hover effect that captures current style to avoid wiping custom overrides
        btn.setOnMouseEntered(e -> {
            // Save the state BEFORE applying hover styles if not already hovering
            if (!btn.getProperties().containsKey("originalStyle")) {
                btn.getProperties().put("originalStyle", btn.getStyle());
            }
            
            String currentStyle = btn.getStyle();
            btn.setStyle(currentStyle + 
                "-fx-background-color: linear-gradient(to right, " + hoverStart + ", " + hoverEnd + ");" +
                "-fx-effect: dropshadow(three-pass-box, " + shadow + ", 12, 0, 0, 4);");
        });
        
        btn.setOnMouseExited(e -> {
            if (btn.getProperties().containsKey("originalStyle")) {
                btn.setStyle((String) btn.getProperties().get("originalStyle"));
                btn.getProperties().remove("originalStyle");
            }
        });

        return btn;
    }
    public static VBox createDialogRoot(double prefWidth) {
        VBox root = new VBox(28);
        root.setPadding(new Insets(35));
        root.setStyle(SOFT_CARD);
        root.setPrefWidth(prefWidth);
        return root;
    }

    public static VBox createDialogHeader(String eyebrow, String title, String subtitle) {
        VBox headerText = new VBox(8);
        Label eb = new Label(eyebrow.toUpperCase());
        eb.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        Label t = new Label(title);
        t.setStyle(DIALOG_TITLE_STYLE);
        Label s = new Label(subtitle);
        s.setStyle(DIALOG_SUBTITLE_STYLE);
        s.setWrapText(true);
        headerText.getChildren().addAll(eb, t, s);
        return headerText;
    }

    public static Button createSecondaryDialogButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(DIALOG_SECONDARY_BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(DIALOG_SECONDARY_BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(DIALOG_SECONDARY_BUTTON_STYLE));
        return btn;
    }

    public static Button createDangerDialogButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(DIALOG_DANGER_BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(DIALOG_DANGER_BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(DIALOG_DANGER_BUTTON_STYLE));
        return btn;
    }

    public static ToggleButton createToggleSwitch(boolean initialState) {
        ToggleButton btn = new ToggleButton();
        btn.setSelected(initialState);
        btn.setPrefSize(48, 28);
        btn.setMinSize(48, 28);
        
        Circle thumb = new Circle(10, Color.WHITE);
        thumb.setEffect(new javafx.scene.effect.DropShadow(javafx.scene.effect.BlurType.THREE_PASS_BOX, Color.web(SHADOW_NAVY_12), 6, 0, 0, 2));
        // Set initial position instantly
        thumb.setTranslateX(initialState ? 20 : 0);
        
        StackPane container = new StackPane(thumb);
        container.setPrefSize(48, 28);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(0, 4, 0, 4));
        
        btn.setGraphic(container);
        
        Runnable updateStyle = () -> {
            boolean selected = btn.isSelected();
            if (selected) {
                container.setStyle(
                    "-fx-background-color: linear-gradient(to right, " + COLOR_GREEN_BRIGHT + ", " + COLOR_SUCCESS + ");" +
                    "-fx-background-radius: 999;" +
                    "-fx-effect: dropshadow(three-pass-box, " + SHADOW_GREEN_20 + ", 12, 0, 0, 2);"
                );
            } else {
                container.setStyle("-fx-background-color: " + COLOR_SLATE_MUTED + "; -fx-background-radius: 999;");
            }

            // Only animate if the switch is already showing on screen
            if (btn.getScene() != null && btn.getScene().getWindow() != null && btn.getScene().getWindow().isShowing()) {
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
                tt.setToX(selected ? 20 : 0);
                tt.play();
            } else {
                thumb.setTranslateX(selected ? 20 : 0);
            }
        };
        
        btn.selectedProperty().addListener((obs, oldVal, newVal) -> updateStyle.run());
        updateStyle.run();
        
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
        return btn;
    }

    public static TextField createStyledField(String val) {
        TextField f = new TextField(val); 
        f.setStyle(PREMIUM_FIELD_STYLE);
        
        f.focusedProperty().addListener((obs, old, newVal) -> {
            if (newVal) {
                f.setStyle(PREMIUM_FIELD_FOCUSED_STYLE);
            } else {
                if (f.getText().trim().isEmpty()) {
                    f.setStyle(PREMIUM_FIELD_ERROR_STYLE);
                } else {
                    f.setStyle(PREMIUM_FIELD_STYLE);
                }
            }
        });

        // Validation on text change as well
        f.textProperty().addListener((obs, old, newVal) -> {
            if (!f.isFocused()) {
                if (newVal.trim().isEmpty()) f.setStyle(PREMIUM_FIELD_ERROR_STYLE);
                else f.setStyle(PREMIUM_FIELD_STYLE);
            }
        });

        return f;
    }

    public static ComboBox<String> createTimeCombo(int max, int current) {
        ComboBox<String> cb = new ComboBox<>();
        for (int i = 0; i < max; i++) cb.getItems().add(String.format("%02d", i));
        cb.setValue(String.format("%02d", current));
        cb.setPrefWidth(95); 
        cb.setPrefHeight(45);
        cb.setMinHeight(45);
        
        // Remove padding from ComboBox to prevent clipping
        cb.setStyle(PREMIUM_SELECT_STYLE + "-fx-font-size: 16px; -fx-padding: 0;"); 
        
        cb.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    // Use internal cell padding to position text without triggering ellipsis
                    setStyle("-fx-text-fill: " + COLOR_NAVY + "; -fx-font-weight: 500; -fx-background-color: transparent; -fx-padding: 0 0 0 10;");
                }
            }
        });
        
        return cb;
    }

    public static Label createChip(String text, String color, String icon) {
        Label l = new Label(text);
        if (icon != null) {
            l.setGraphic(createSVGIcon(icon, Color.web(color), 12));
            l.setGraphicTextGap(8);
        }
        l.setStyle(
                "-fx-background-color: " + color + "10;" +
                "-fx-text-fill: " + color + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: 900;" +
                "-fx-padding: 6 14;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + color + "25;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;"
        );
        return l;
    }

    public static HBox createFieldRow(String label, TextField field) {
        HBox h = new HBox(15, new Label(label), new Region(), field); 
        HBox.setHgrow(h.getChildren().get(1), Priority.ALWAYS);
        h.setAlignment(Pos.CENTER_LEFT); 
        return h;
    }

    public static VBox createModernSettingsGroup(String title, String icon, String color, Node content) {
        VBox section = new VBox(25);
        section.setPadding(new Insets(30)); // Add padding!
        section.setStyle(SOFT_CARD);
        HBox.setHgrow(section, Priority.ALWAYS);

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(color), 22));
        iconBox.setAlignment(Pos.CENTER); // Explicit center alignment
        iconBox.setPrefSize(54, 54);
        iconBox.setMinSize(54, 54);
        iconBox.setStyle(ICON_BADGE_STYLE);

        VBox titleBlock = new VBox(1);
        Label tagLabel = new Label("ПАРАМЕТРИ");
        tagLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE_LIGHT + "; -fx-letter-spacing: 1.2px;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        titleBlock.getChildren().addAll(tagLabel, titleLabel);

        header.getChildren().addAll(iconBox, titleBlock);
        section.getChildren().addAll(header, content);
        return section;
    }

    public static VBox createLabeledField(String label, Node field) {
        VBox group = new VBox(8);
        if (field instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        
        Label l = new Label(label.toUpperCase());
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE_LIGHT + "; -fx-letter-spacing: 1.2px;");
        
        group.getChildren().addAll(l, field);
        VBox.setVgrow(field, Priority.ALWAYS);
        return group;
    }

    public static VBox createSettingsSection(String title, String color, String svgPath) {
        VBox v = new VBox(12); 
        v.setPadding(new Insets(15)); 
        v.setStyle(SOFT_CARD);
        Label titleLabel = new Label(title);
        titleLabel.setStyle(HEADER_STYLE);
        HBox header = new HBox(12, createSVGIcon(svgPath, Color.web(color), 20), titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        v.getChildren().add(header); 
        return v;
    }

    public static Button createActionButton(String title, String subtext, String iconPath, String gradient) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(30, 35, 30, 35));
        btn.setCache(true);
        btn.setCacheHint(javafx.scene.CacheHint.QUALITY);
        
        btn.getProperties().put("baseGradient", gradient);
        applyActionButtonStyle(btn, false);
        
        BorderPane content = new BorderPane();
        
        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.WHITE, 44));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(74, 74);
        iconBox.setStyle("-fx-background-color: " + TR_WHITE_15 + "; " +
                          "-fx-background-radius: 22; " +
                          "-fx-border-color: " + TR_WHITE_30 + "; " +
                          "-fx-border-width: 1.5; " +
                          "-fx-border-radius: 22;");
        
        VBox text = new VBox(4);
        text.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title); 
        t.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: white; -fx-letter-spacing: 1px;");
        
        Label s = new Label(subtext); 
        s.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TR_WHITE_80 + "; -fx-font-weight: 700;");
        text.getChildren().addAll(t, s);
        
        HBox leftSide = new HBox(25, iconBox, text);
        leftSide.setAlignment(Pos.CENTER_LEFT);
        
        String arrowPath = "M4,11V13H16L10.5,18.5L11.92,19.92L19.84,12L11.92,4.08L10.5,5.5L16,11H4Z";
        Node arrowIcon = createSVGIcon(arrowPath, Color.WHITE, 32);
        
        content.setLeft(leftSide);
        content.setRight(arrowIcon);
        BorderPane.setAlignment(arrowIcon, Pos.CENTER);
        
        btn.setGraphic(content);
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
        btn.setOnMouseEntered(e -> {
            applyActionButtonStyle(btn, true);
            btn.setTranslateY(-6);
            iconBox.setStyle(iconBox.getStyle().replace(TR_WHITE_15, TR_WHITE_28));
            arrowIcon.setTranslateX(10);
            arrowIcon.setScaleX(1.15);
            arrowIcon.setScaleY(1.15);
        });
        
        btn.setOnMouseExited(e -> {
            applyActionButtonStyle(btn, false);
            btn.setTranslateY(0);
            iconBox.setStyle(iconBox.getStyle().replace(TR_WHITE_28, TR_WHITE_15));
            arrowIcon.setTranslateX(0);
            arrowIcon.setScaleX(1.0);
            arrowIcon.setScaleY(1.0);
        });
        
        btn.setOnMousePressed(e -> btn.setTranslateY(-1));
        
        return btn;
    }

    public static void applyActionButtonStyle(Button btn, boolean hover) {
        String gradient = (String) btn.getProperties().getOrDefault("baseGradient", GRADIENT_PRIMARY);
        String style = "-fx-background-color: " + gradient + "; " +
                          "-fx-background-radius: 28; " +
                          "-fx-cursor: hand; " +
                          "-fx-border-color: " + TR_WHITE_25 + "; " +
                          "-fx-border-width: 1.5; " +
                          "-fx-border-radius: 28;";
        if (hover) {
            style += "-fx-effect: dropshadow(three-pass-box, " + SHADOW_BLACK_30 + ", 25, 0, 0, 10);";
        } else {
            style += "-fx-effect: dropshadow(three-pass-box, " + SHADOW_BLACK_18 + ", 15, 0, 0, 8);";
        }
        btn.setStyle(style);
    }

    public static void updateActionButton(Button btn, String title, String subtext, String iconPath, String gradient) {
        BorderPane content = (BorderPane) btn.getGraphic();
        HBox leftSide = (HBox) content.getLeft();
        VBox iconBox = (VBox) leftSide.getChildren().get(0);
        VBox textStack = (VBox) leftSide.getChildren().get(1);
        
        Label t = (Label) textStack.getChildren().get(0);
        Label s = (Label) textStack.getChildren().get(1);
        
        if (!t.getText().equals(title)) {
            t.setText(title);
            s.setText(subtext);
            iconBox.getChildren().clear();
            iconBox.getChildren().add(createSVGIcon(iconPath, Color.WHITE, 42));
            
            btn.getProperties().put("baseGradient", gradient);
            applyActionButtonStyle(btn, false);
        }
    }

    public static HBox createSegmentedFilter(String leftText, String rightText, boolean initialState, java.util.function.Consumer<Boolean> onToggle) {
        HBox container = new HBox(0);
        container.setAlignment(Pos.CENTER);
        // Height 44px, Width 280px, Radius 22px - Identical to SystemView Relay Toggle for consistency
        container.setStyle(PREMIUM_TOGGLE_CONTAINER + "-fx-background-color: white; -fx-background-radius: 22; -fx-padding: 2; -fx-border-color: " + BORDER_SLATE_70 + "; -fx-border-radius: 22; -fx-border-width: 1;");
        container.setMinWidth(280);
        container.setMaxWidth(280);
        container.setPrefHeight(44);
        container.setMinHeight(44);
        container.setMaxHeight(44);

        Button leftBtn = new Button(leftText.toUpperCase());
        Button rightBtn = new Button(rightText.toUpperCase());

        leftBtn.setPrefHeight(Double.MAX_VALUE);
        rightBtn.setPrefHeight(Double.MAX_VALUE);
        HBox.setHgrow(leftBtn, Priority.ALWAYS);
        HBox.setHgrow(rightBtn, Priority.ALWAYS);
        leftBtn.setMaxWidth(Double.MAX_VALUE);
        rightBtn.setMaxWidth(Double.MAX_VALUE);

        // State tracking
        final boolean[] isRight = {initialState};

        java.lang.Runnable updateStyles = () -> {
            // Font size 13px, weight 900 to match the "Small Primary Action Button"
            String baseBtnStyle = "-fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 0 20; -fx-cursor: hand; -fx-letter-spacing: 0.5px;";
            String activeStyle = baseBtnStyle + "-fx-background-color: linear-gradient(to right, " + COLOR_INDIGO + ", " + COLOR_PRIMARY + "); -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, " + SHADOW_INDIGO_28 + ", 10, 0, 0, 3);";
            String inactiveStyle = baseBtnStyle + "-fx-background-color: transparent; -fx-text-fill: " + COLOR_SLATE + "; -fx-effect: null;";
            
            leftBtn.setStyle(isRight[0] ? inactiveStyle : activeStyle);
            rightBtn.setStyle(isRight[0] ? activeStyle : inactiveStyle);
            
            // Perfect radii for segmented pill look (20px inside 22px container)
            leftBtn.setStyle(leftBtn.getStyle() + "-fx-background-radius: 20 0 0 20;");
            rightBtn.setStyle(rightBtn.getStyle() + "-fx-background-radius: 0 20 20 0;");
        };

        leftBtn.setOnAction(e -> {
            if (isRight[0]) {
                isRight[0] = false;
                updateStyles.run();
                onToggle.accept(false);
            }
        });

        rightBtn.setOnAction(e -> {
            if (!isRight[0]) {
                isRight[0] = true;
                updateStyles.run();
                onToggle.accept(true);
            }
        });

        updateStyles.run();
        container.getChildren().addAll(leftBtn, rightBtn);
        return container;
    }

    public static VBox createEmptyState(String iconPath, String title, String subtitle) {
        VBox empty = new VBox(20);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(100, 40, 100, 40));
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.setStyle("-fx-alignment: center;");
        HBox.setHgrow(empty, Priority.ALWAYS);

        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(COLOR_WHITE_MUTED_BORDER), 64));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setStyle("-fx-alignment: center;");
        
        VBox text = new VBox(8);
        text.setAlignment(Pos.CENTER);
        text.setStyle("-fx-alignment: center;");
        
        Label t = new Label(title);
        t.setMaxWidth(Double.MAX_VALUE);
        t.setAlignment(Pos.CENTER);
        t.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_ICON_MUTED + "; -fx-text-alignment: center;");
        
        Label s = new Label(subtitle);
        s.setMaxWidth(Double.MAX_VALUE);
        s.setAlignment(Pos.CENTER);
        s.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_SLATE + "; -fx-text-alignment: center;");
        s.setWrapText(true);
        s.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        s.setMaxWidth(450);
        
        text.getChildren().addAll(t, s);
        empty.getChildren().addAll(iconBox, text);
        return empty;
    }
}
