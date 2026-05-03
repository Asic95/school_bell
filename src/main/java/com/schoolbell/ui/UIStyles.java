package com.schoolbell.ui;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class UIStyles {
    // Colors and Effects
    public static final String DEPTH_1 = "-fx-background-color: #f1f2f6;";
    public static final String DEPTH_2 = "-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);";
    public static final String DEPTH_3 = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 2);";

    // Styles
    public static final String HEADER_STYLE = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #95a5a6; -fx-letter-spacing: 1.5px;";
    public static final String VALUE_STYLE = "-fx-font-size: 26px; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-text-fill: #2d3436;";
    public static final String BTN_BASE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;";
    public static final String COMBO_STYLE = "-fx-font-size: 14px; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 8; -fx-border-color: #dfe6e9; -fx-border-radius: 8; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;";

    public static final String TAB_STYLE =
        ".tab-pane { -fx-focus-color: transparent; -fx-faint-focus-color: transparent; }" +
        ".tab-pane .tab-header-area .tab-header-background { -fx-opacity: 0; }" +
        ".tab { -fx-background-color: #e1e2e1; -fx-background-radius: 10 10 0 0; -fx-padding: 10 25; }" +
        ".tab:selected { -fx-background-color: white; -fx-background-insets: 0; }" +
        ".tab .tab-label { -fx-text-fill: #636e72; -fx-font-weight: bold; -fx-font-size: 12px; }" +
        ".tab:selected .tab-label { -fx-text-fill: #2d3436; }" +
        ".tab:focused .tab-label { -fx-focus-color: transparent; }" +
        ".teacher-card { -fx-background-color: #f1f7ff; -fx-background-radius: 12; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-border-color: #d1e3ff; -fx-border-radius: 12; }" +
        ".subject-card { -fx-background-color: #f1f9f6; -fx-background-radius: 12; -fx-padding: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-border-color: #d1f2e9; -fx-border-radius: 12; }" +
        ".class-card { -fx-background-color: #f6f1ff; -fx-background-radius: 12; -fx-padding: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-border-color: #e9d1ff; -fx-border-radius: 12; }" +
        ".subject-chip { -fx-background-color: white; -fx-background-radius: 15; -fx-padding: 4 10; -fx-font-size: 11px; -fx-text-fill: #2d3436; -fx-border-color: #dfe6e9; -fx-border-radius: 15; }" +
        ".subject-chip:hover { -fx-background-color: #ff7675; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #ff7675; }";

    // SVG Icons
    public static final String ICON_BELL = "M12,2A2,2 0 0,0 10,4A2,2 0 0,0 10,4.29C7.12,5.14 5,7.82 5,11V17L3,19V20H21V19L19,17V11C19,7.82 16.88,5.14 14,4.29C14,4.19 14,4.1 14,4A2,2 0 0,0 12,2M10,21A2,2 0 0,0 12,23A2,2 0 0,0 14,21H10Z";
    public static final String ICON_MEGAPHONE = "M12,18H10L9,21H7L8,18H5C3.9,18 3,17.1 3,16V9C3,7.9 3.9,7 5,7H11L18,2V21L11,16M11,14V9H5V16H8.4L11,14Z";
    public static final String ICON_ALERT = "M13,14H11V9H13M13,18H11V16H13M1,21H23L12,2L1,21Z";
    public static final String ICON_SETTINGS = "M12,15.5A3.5,3.5 0 0,1 8.5,12A3.5,3.5 0 0,1 12,8.5A3.5,3.5 0 0,1 15.5,12A3.5,3.5 0 0,1 12,15.5M19.43,12.97C19.47,12.65 19.5,12.33 19.5,12C19.5,11.67 19.47,11.35 19.43,11.03L21.54,9.37C21.73,9.22 21.78,8.95 21.66,8.73L19.66,5.27C19.54,5.05 19.27,4.97 19.05,5.05L16.56,5.9C16.04,5.5 15.48,5.19 14.88,4.97L14.5,2.33C14.46,2.1 14.26,1.92 14.03,1.92H10.03C9.8,1.92 9.6,2.1 9.57,2.33L9.18,4.97C8.58,5.19 8.02,5.5 7.5,5.9L5.01,5.05C4.79,4.97 4.52,5.05 4.4,5.27L2.4,8.73C2.28,8.95 2.33,9.22 2.52,9.37L4.63,11.03C4.59,11.35 4.56,11.67 4.56,12C4.56,12.33 4.59,12.65 4.63,12.97L2.52,14.63C2.33,14.78 2.28,15.05 2.4,15.27L4.4,18.73C4.52,18.95 4.79,19.03 5.01,18.95L7.5,18.1C8.02,18.5 8.58,18.81 9.18,19.03L9.57,21.67C9.6,21.9 9.8,22.08 10.03,22.08H14.03C14.26,22.08 14.46,21.9 14.5,21.67L14.88,19.03C15.48,18.81 16.04,18.5 16.56,18.1L19.05,18.95C19.27,19.03 19.54,18.95 19.66,18.73L21.66,15.27C21.78,15.05 21.73,14.78 21.54,14.63L19.43,12.97Z";
    public static final String ICON_CALENDAR = "M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.9 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z";
    public static final String ICON_SAVE = "M17,3L21,7V19A2,2 0 0,1 19,21H5C3.89,21 3,20.1 3,19V5A2,2 0 0,1 5,3H17M12,12A3,3 0 0,0 9,15A3,3 0 0,0 12,18A3,3 0 0,0 15,15A3,3 0 0,0 12,12M15,5H5V9H15V5Z";
    public static final String ICON_MUSIC = "M21,3V15.5A3.5,3.5 0 0,1 17.5,19A3.5,3.5 0 0,1 14,15.5A3.5,3.5 0 0,1 17.5,12C18.04,12 18.55,12.12 19,12.34V6.47L9,8.6V17.5A3.5,3.5 0 0,1 5.5,21A3.5,3.5 0 0,1 2,17.5A3.5,3.5 0 0,1 5.5,14C6.04,14 6.55,14.12 7,14.34V4.53L21,2V3Z";
    public static final String ICON_FOLDER = "M10,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V8C22,6.89 21.1,6 20,6H12L10,4Z";
    public static final String ICON_PERSON = "M12,12c2.21,0,4-1.79,4-4s-1.79-4-4-4-4,1.79-4,4,1.79,4,4,4zm0,2c-2.67,0-8,1.34-8,4v2h16v-2c0-2.66-5.33-4-8-4z";
    public static final String ICON_BOOK = "M18,2H6c-1.1,0-2,0.9-2,2v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V4c0-1.1-0.9-2-2-2zM6,4h5v8l-2.5-1.5L6,12V4z";
    public static final String ICON_CLASS = "M12,3L1,9l11,6l9-4.91V17h2V9L12,3zM3.89,9L12,4.57L20.11,9L12,13.43L3.89,9z";

    public static Node createSVGIcon(String pathData, Color color, double size) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.setFill(color);
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }
}
