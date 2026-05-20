package com.schoolbell.ui;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class UIStyles {
    // Colors (Zinc-based palette)
    public static final String COLOR_BG = "#f9f9fb";
    public static final String COLOR_WHITE = "#ffffff";
    public static final String COLOR_ZINC_200 = "#e4e4e7";
    public static final String COLOR_ZINC_500 = "#71717a";
    public static final String COLOR_ZINC_900 = "#18181b";


    public static final String COLOR_PRIMARY = "#2563eb";
    public static final String COLOR_SUCCESS = "#16a34a";
    public static final String COLOR_WARNING = "#d97706";
    public static final String COLOR_DANGER = "#dc2626";
    public static final String COLOR_NEUTRAL = "#636e72";
    public static final String COLOR_TEXT = "#2d3436";
    public static final String COLOR_TEXT_DIM = "#95a5a6";
    public static final String COLOR_PURPLE = "#a29bfe";
    public static final String COLOR_BLUE_LIGHT = "#e3f2fd";
    public static final String COLOR_GREEN_LIGHT = "#e8f8f5";
    public static final String COLOR_PURPLE_LIGHT = "#f3efff";

    public static final String DEPTH_1 = "-fx-background-color: #f1f2f6;";
    public static final String DEPTH_2 = "-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 3);";
    public static final String SOFT_CARD = "-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e4e4e7; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);";
    public static final String HEADER_STYLE = "-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #64748b; -fx-text-transform: uppercase; -fx-letter-spacing: 1.5px;";
    public static final String SUB_HEADER_STYLE = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + "; -fx-letter-spacing: 0.5px;";

    public static final String VALUE_STYLE = "-fx-font-size: 26px; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + ";";
    public static final String BTN_BASE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;";
    public static final String COMBO_STYLE = "-fx-font-size: 14px; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 8; -fx-border-color: #dfe6e9; -fx-border-radius: 8; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;";
    public static final String FIELD_STYLE = "-fx-font-size: 14px; -fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #dfe6e9; -fx-border-radius: 8; -fx-padding: 8;";

    public static final String MODERN_SPINNER_STYLE = 
        ".spinner { -fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #dfe6e9; -fx-border-radius: 8; } " +
        ".spinner > .text-field { -fx-font-size: 14px; -fx-background-color: white; -fx-background-radius: 8 0 0 8; -fx-background-insets: 0; -fx-padding: 8; } " +
        ".spinner > .increment-arrow-button, .spinner > .decrement-arrow-button { -fx-background-color: #f1f2f6; -fx-background-insets: 0; -fx-cursor: hand; } " +
        ".spinner > .increment-arrow-button { -fx-background-radius: 0 8 0 0; } " +
        ".spinner > .decrement-arrow-button { -fx-background-radius: 0 0 8 0; } " +
        ".spinner > .increment-arrow-button:hover, .spinner > .decrement-arrow-button:hover { -fx-background-color: " + COLOR_PRIMARY + "22; } " +
        ".spinner > .increment-arrow-button > .increment-arrow { -fx-shape: \"M7.41,15.41L12,10.83L16.59,15.41L18,14L12,8L6,14L7.41,15.41Z\"; -fx-background-color: " + COLOR_PRIMARY + "; -fx-scale-x: 0.8; -fx-scale-y: 0.8; } " +
        ".spinner > .decrement-arrow-button > .decrement-arrow { -fx-shape: \"M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z\"; -fx-background-color: " + COLOR_PRIMARY + "; -fx-scale-x: 0.8; -fx-scale-y: 0.8; } ";

    public static final String TAB_STYLE = ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: transparent; } " +
            ".tab-pane .tab { -fx-background-color: #e1e4e8; -fx-background-radius: 10 10 0 0; -fx-padding: 8 20; -fx-background-insets: 0 1 0 1; } " +
            ".tab-pane .tab:selected { -fx-background-color: white; -fx-background-radius: 10 10 0 0; } " +
            ".tab-pane .tab .tab-label { -fx-text-fill: #57606f; -fx-font-weight: bold; } " +
            ".tab-pane .tab:selected .tab-label { -fx-text-fill: " + COLOR_PRIMARY + "; }";

    public static final String SUBJECT_CHIP_STYLE = ".subject-chip { -fx-background-color: white; -fx-background-radius: 15; -fx-padding: 4 10; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT + "; -fx-border-color: #dfe6e9; -fx-border-radius: 15; } .subject-chip:hover { -fx-background-color: #ff7675; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #ff7675; }";

    public static final String SIDEBAR_STYLE = "-fx-background-color: #0c1427; -fx-padding: 20 0 20 0;";
    public static final String NAV_BTN_BASE = "-fx-background-color: transparent; -fx-padding: 12 20; -fx-cursor: hand; -fx-background-radius: 12; -fx-text-fill: #b2bec3; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-font-size: 14px;";
    public static final String NAV_BTN_ACTIVE = "-fx-background-color: #0984e3; -fx-text-fill: white;";
    public static final String NAV_BTN_HOVER = "-fx-background-color: rgba(255,255,255,0.05);";

    public static final double ICON_SCALE = 1.0;

    public static final String SIDEBAR_STATUS_STYLE = "-fx-background-color: #0c1427; -fx-padding: 20; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 1 0 0 0;";

    public static final String MODERN_DATE_PICKER_STYLE = 
        ".date-picker { -fx-background-color: transparent; } " +
        ".date-picker > .text-field { -fx-font-size: 14px; -fx-background-color: white; -fx-background-radius: 12 0 0 12; -fx-border-color: #dfe6e9; -fx-border-radius: 12 0 0 12; -fx-border-width: 1 0 1 1; -fx-padding: 8; } " +
        ".date-picker > .arrow-button { -fx-background-color: " + COLOR_PRIMARY + "; -fx-background-radius: 0 12 12 0; -fx-padding: 8; -fx-cursor: hand; } " +
        ".date-picker > .arrow-button > .arrow { -fx-shape: \"M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.9 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z\"; -fx-background-color: white; -fx-padding: 8; } " +
        ".date-picker-popup { -fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 12, 0, 0, 8); -fx-border-color: #dfe6e9; -fx-border-width: 1; -fx-border-radius: 16; } " +
        ".date-picker-popup > .month-year-pane { -fx-background-color: white; -fx-background-radius: 16 16 0 0; -fx-padding: 10; } " +
        ".date-picker-popup > .calendar-grid { -fx-background-color: white; -fx-padding: 10; } " +
        ".date-picker-popup > * > .spinner > .button { -fx-background-color: transparent; } " +
        ".date-picker-popup > * > .spinner > .button:hover { -fx-background-color: #f1f2f6; } " +
        ".date-picker-popup > * > .day-name-cell, .date-picker-popup > * > .week-number-cell { -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #95a5a6; -fx-padding: 8; } " +
        ".date-picker-popup > * > .day-cell { -fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8; -fx-cursor: hand; -fx-text-fill: " + COLOR_TEXT + "; } " +
        ".date-picker-popup > * > .day-cell:hover { -fx-background-color: #e3f2fd; -fx-text-fill: " + COLOR_PRIMARY + "; } " +
        ".date-picker-popup > * > .today { -fx-background-color: #f8f9fa; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-weight: bold; -fx-border-color: " + COLOR_PRIMARY + "44; -fx-border-radius: 8; } " +
        ".date-picker-popup > * > .selected { -fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white; -fx-font-weight: bold; } " +
        ".date-picker-popup > * > .selected:hover { -fx-text-fill: white; } " +
        ".date-picker-popup > * > .previous-month, .date-picker-popup > * > .next-month { -fx-text-fill: #bdc3c7; }";

    // SVG Icons
    public static final String ICON_DASHBOARD = "M13,3V9H21V3H13M13,21H21V15H13V21M3,21H11V11H3V21M3,9H11V3H3V9Z";
    public static final String ICON_BELL = "M12,2A2,2 0 0,0 10,4A2,2 0 0,0 10,4.29C7.12,5.14 5,7.82 5,11V17L3,19V20H21V19L19,17V11C19,7.82 16.88,5.14 14,4.29C14,4.19 14,4.1 14,4A2,2 0 0,0 12,2M10,21A2,2 0 0,0 12,23A2,2 0 0,0 14,21H10Z";
    public static final String ICON_MEGAPHONE = "M12,18H10L9,21H7L8,18H5C3.9,18 3,17.1 3,16V9C3,7.9 3.9,7 5,7H11L18,2V21L11,16M11,14V9H5V16H8.4L11,14Z";
    public static final String ICON_ALERT = "M12,2L2,21A2,2 0 0,0 4,24H20A2,2 0 0,0 22,21L12,2M12,6L18.53,19H5.47L12,6M13,15H11V17H13V15M13,10H11V14H13V10Z";
    public static final String ICON_SETTINGS = "M12,15.5A3.5,3.5 0 0,1 8.5,12A3.5,3.5 0 0,1 12,8.5A3.5,3.5 0 0,1 15.5,12A3.5,3.5 0 0,1 12,15.5M19.43,12.97C19.47,12.65 19.5,12.33 19.5,12C19.5,11.67 19.47,11.35 19.43,11.03L21.54,9.37C21.73,9.22 21.78,8.95 21.66,8.73L19.66,5.27C19.54,5.05 19.27,4.97 19.05,5.05L16.56,5.9C16.04,5.5 15.48,5.19 14.88,4.97L14.5,2.33C14.46,2.1 14.26,1.92 14.03,1.92H10.03C9.8,1.92 9.6,2.1 9.57,2.33L9.18,4.97C8.58,5.19 8.02,5.5 7.5,5.9L5.01,5.05C4.79,4.97 4.52,5.05 4.4,5.27L2.4,8.73C2.28,8.95 2.33,9.22 2.52,9.37L4.63,11.03C4.59,11.35 4.56,11.67 4.56,12C4.56,12.33 4.59,12.65 4.63,12.97L2.52,14.63C2.33,14.78 2.28,15.05 2.4,15.27L4.4,18.73C4.52,18.95 4.79,19.03 5.01,18.95L7.5,18.1C8.02,18.5 8.58,18.81 9.18,19.03L9.57,21.67C9.6,21.9 9.8,22.08 10.03,22.08H14.03C14.26,22.08 14.46,21.9 14.5,21.67L14.88,19.03C15.48,18.81 16.04,18.5 16.56,18.1L19.05,18.95C19.27,19.03 19.54,18.95 19.66,18.73L21.66,15.27C21.78,15.05 21.73,14.78 21.54,14.63L19.43,12.97Z";
    public static final String ICON_CALENDAR = "M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.9 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z";
    public static final String ICON_NOTIFICATIONS = "M21,19V20H3V19L5,17V11C5,7.9 7.1,5.2 10,4.3V4A2,2 0 0,1 12,2A2,2 0 0,1 14,4V4.3C16.9,5.2 19,7.9 19,11V17L21,19M12,22A2,2 0 0,0 14,20H10A2,2 0 0,0 12,22Z";
    public static final String ICON_SIGNAL = "M12,3C7.79,3 3.7,4.41 0.38,7H0.38L12,21L23.65,7H23.65C20.3,4.41 16.21,3 12,3Z";
    public static final String ICON_BROADCAST = "M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4M12,6A6,6 0 0,0 6,12A6,6 0 0,0 12,18A6,6 0 0,0 18,12A6,6 0 0,0 12,6M12,8A4,4 0 0,1 16,12A4,4 0 0,1 12,16A4,4 0 0,1 8,12A4,4 0 0,1 12,8Z";
    public static final String ICON_SAVE = "M17,3L21,7V19A2,2 0 0,1 19,21H5C3.89,21 3,20.1 3,19V5A2,2 0 0,1 5,3H17M12,12A3,3 0 0,0 9,15A3,3 0 0,0 12,18A3,3 0 0,0 15,15A3,3 0 0,0 12,12M15,5H5V9H15V5Z";
    public static final String ICON_MUSIC = "M21,3V15.5A3.5,3.5 0 0,1 17.5,19A3.5,3.5 0 0,1 14,15.5A3.5,3.5 0 0,1 17.5,12C18.04,12 18.55,12.12 19,12.34V6.47L9,8.6V17.5A3.5,3.5 0 0,1 5.5,21A3.5,3.5 0 0,1 2,17.5A3.5,3.5 0 0,1 5.5,14C6.04,14 6.55,14.12 7,14.34V4.53L21,2V3Z";
    public static final String ICON_FOLDER = "M10,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V8C22,6.89 21.1,6 20,6H12L10,4Z";
    public static final String ICON_PERSON = "M12,12c2.21,0,4-1.79,4-4s-1.79-4-4-4-4,1.79-4,4,1.79,4,4,4zm0,2c-2.67,0-8,1.34-8,4v2h16v-2c0-2.66-5.33-4-8-4z";
    public static final String ICON_BOOK = "M18,2H6c-1.1,0-2,0.9-2,2v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V4c0-1.1-0.9-2-2-2zM6,4h5v8l-2.5-1.5L6,12V4z";
    public static final String ICON_CLASS = "M12,3L1,9l11,6l9-4.91V17h2V9L12,3zM3.89,9L12,4.57L20.11,9L12,13.43L3.89,9z";
    public static final String ICON_CLOCK = "M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z";
    public static final String ICON_CLOCK_FAST = "M15,1H9V3H15V1M11,14H13V8H11V14M19.03,7.39L20.45,5.97C20,5.46 19.55,5 19.04,4.56L17.62,5.97C15.91,4.74 13.82,4 12,4A9,9 0 0,0 3,13A9,9 0 0,0 12,22C17,22 21,17.97 21,13C21,10.88 20.26,8.93 19.03,7.39M12,20A7,7 0 0,1 5,13A7,7 0 0,1 12,6A7,7 0 0,1 19,13A7,7 0 0,1 12,20Z";
    public static final String ICON_ROOM = "M19 19V5c0-1.1-.9-2-2-2H7c-1.1 0-2 .9-2 2v14H3v2h18v-2h-2zm-4-6h-2v-2h2v2z";
    public static final String ICON_INFO = "M11,9H13V7H11M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M11,17H13V11H11V17Z";
    public static final String ICON_MESSAGE = "M20,2H4A2,2 0 0,0 2,4V22L6,18H20A2,2 0 0,0 22,16V4A2,2 0 0,0 20,2M20,16H5.17L4,17.17V4H20V16Z";
    public static final String ICON_VOLUME = "M14,3.23V5.29C16.89,6.15 19,8.83 19,12C19,15.17 16.89,17.85 14,18.71V20.77C18,19.86 21,16.28 21,12C21,7.72 18,4.14 14,3.23M16.5,12C16.5,10.23 15.5,8.71 14,7.97V16.03C15.5,15.29 16.5,13.77 16.5,12M3,9V15H7L12,20V4L7,9H3Z";
    public static final String ICON_MONITOR = "M21,14H22V16H2V14H3V4H21V14M19,12V6H5V12H19M14,18H10V17H14V18M15,20H9V19H15V20Z";
    public static final String ICON_PHONE = "M17,1H7A2,2 0 0,0 5,3V21A2,2 0 0,0 7,23H17A2,2 0 0,0 19,21V3A2,2 0 0,0 17,1M17,19H7V5H17V19Z";
    public static final String ICON_TABLET = "M19,18H5V6H19M21,4H3C1.89,4 1,4.89 1,6V18A2,2 0 0,0 3,20H21A2,2 0 0,0 23,18V6C23,4.89 22.1,4 21,4Z";
    public static final String ICON_PLUS = "M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z";
    public static final String ICON_REFRESH = "M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z";
    public static final String ICON_EDIT = "M20.71,7.04C21.1,6.65 21.1,6.01 20.71,5.63L18.37,3.29C17.99,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z";
    public static final String ICON_TRASH = "M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19V4M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z";
    public static final String ICON_SCHOOL = "M12,3L1,9L12,15L21,10.09V17H23V9M12,4.8L19.2,8.8L12,12.8L4.8,8.8L12,4.8M5,10.8V15.5L12,19.3L19,15.5V10.8L12,14.7L5,10.8Z";
    public static final String ICON_BAN = "M12,2A10,10 0 1,0 22,12A10,10 0 0,0 12,2M12,4A8,8 0 0,1 18,12A8,8 0 0,1 17.41,15.59L8.41,6.59A8,8 0 0,1 12,4M12,20A8,8 0 0,1 6,12A8,8 0 0,1 6.59,8.41L15.59,17.41A8,8 0 0,1 12,20Z";
    public static final String ICON_CHECK = "M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z";
    public static final String ICON_DANGER = "M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z";
    public static final String ICON_AIR_RAID = "M12,2L1,21H23L12,2M12,6L19.53,19H4.47L12,6M13,16H11V18H13V16M13,10H11V14H13V10Z";
    public static final String ICON_WAVEFORM = "M2,14H4V16H2V14M6,10H8V20H6V10M10,4H12V22H10V4M14,12H16V18H14V12M18,8H20V20H18V8M22,14H24V16H22V14Z";
    public static final String ICON_LIFEBUOY = "M12,1L3,5v6c0,5.55,3.84,10.74,9,12c5.16,-1.26,9,-6.45,9,-12V5L12,1z M11,7h2v7h-2V7z M11,16h2v2h-2V16z";
    public static final String ICON_ALL_CLEAR = "M10,17L5,12L6.41,10.58L10,14.17L17.59,6.58L19,8L10,17Z";
    public static final String ICON_EXTERNAL_LINK = "M14,3V5H17.59L7.76,14.83L9.17,16.24L19,6.41V10H21V3M19,19H5V5H12V3H5C3.89,3 3,3.9 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V12H19V19Z";
    public static final String ICON_SHIELD = "M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1M12,3L19,6.09V11C19,15.47 16.02,19.66 12,20.9C7.98,19.66 5,15.47 5,11V6.09L12,3Z";
    public static final String ICON_NET = "M12,11L12,13H10V11H12M18,11L18,13H16V11H18M12,17L12,19H10V17H12M18,17L18,19H16V17H18M20,15V21H4V15H2V13H4V11H2V9H4V3H20V9H22V11H20V13H22V15H20M18,5H6V9H18V5M18,15H6V19H18V15Z";
    public static final String ICON_LINK = "M3.9,12C3.9,10.29 5.29,8.9 7,8.9H11V7H7A5,5 0 0,0 2,12A5,5 0 0,0 7,17H11V15.1H7C5.29,15.1 3.9,13.71 3.9,12M8,13H16V11H8V13M17,7H13V8.9H17C18.71,8.9 20.1,10.29 20.1,12C20.1,13.71 18.71,15.1 17,15.1H13V17H17A5,5 0 0,0 22,12A5,5 0 0,0 17,7Z";
    public static final String ICON_CHEVRON_RIGHT = "M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z";
    public static final String ICON_CLONE = "M19,21H8V7H19M19,5H8A2,2 0 0,0 6,7V21A2,2 0 0,0 8,23H19A2,2 0 0,0 21,21V7A2,2 0 0,0 19,5M16,1H4A2,2 0 0,0 2,3V17H4V3H16V1Z";
    public static final String ICON_PAUSE = "M14,19H18V5H14M6,19H10V5H6V19Z";
    public static final String ICON_PLAY = "M8,5.14V19.14L19,12.14L8,5.14Z";

    
    // New Modern Settings Icons
    public static final String ICON_POWER = "M16.56,5.44L15.11,6.89C16.84,7.94 18,9.83 18,12A6,6 0 0,1 12,18A6,6 0 0,1 6,12C6,9.83 7.16,7.94 8.88,6.88L7.44,5.44C5.13,6.96 3.5,9.6 3.5,12.6C3.5,17.3 7.3,21.1 12,21.1C16.7,21.1 20.5,17.3 20.5,12.6C20.5,9.6 18.87,6.96 16.56,5.44M13,3H11V13H13V3Z";
    public static final String ICON_TRAY = "M2,12H4V17H20V12H22V17A2,2 0 0,1 20,19H4A2,2 0 0,1 2,17V12M12,15L17.5,9.5L16.08,8.08L13,11.17V2H11V11.17L7.92,8.08L6.5,9.5L12,15Z";
    public static final String ICON_FLASK = "M20.3,19.1L15.1,9.7L15,9.5V4H17V2H7V4H9V9.5L8.9,9.7L3.7,19.1C3.4,19.7 3.8,22 5,22H19C20.2,22 20.6,19.7 20.3,19.1Z";
    public static final String ICON_PALETTE = "M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A1,1 0 0,0 13,21V20C13,18.89 13.9,18 15,18H17A5,5 0 0,0 22,13C22,6.92 17.5,2 12,2M6.5,10A1.5,1.5 0 1,1 5,11.5A1.5,1.5 0 0,1 6.5,10M9.5,6A1.5,1.5 0 1,1 8,7.5A1.5,1.5 0 0,1 9.5,6M14.5,6A1.5,1.5 0 1,1 13,7.5A1.5,1.5 0 0,1 14.5,6M17.5,10A1.5,1.5 0 1,1 16,11.5A1.5,1.5 0 0,1 17.5,10Z";
    
    // Premium Gradients
    public static final String GRADIENT_PRIMARY = "linear-gradient(to bottom right, #0984e3, #2980b9)";
    public static final String GRADIENT_WARNING = "linear-gradient(to bottom right, #ff9f43, #f39c12)";
    public static final String GRADIENT_DANGER = "linear-gradient(to bottom right, #e74c3c, #c0392b)";
    public static final String GRADIENT_SUCCESS = "linear-gradient(to bottom right, #2ecc71, #27ae60)";
    public static final String GRADIENT_INFO = "linear-gradient(to bottom right, #3498db, #2980b9)";
    public static final String GRADIENT_PURPLE = "linear-gradient(to bottom right, #9b59b6, #8e44ad)";
    public static final String GRADIENT_NEUTRAL = "linear-gradient(to bottom right, #636e72, #2d3436)";

    public static final String MODERN_CHECKBOX_STYLE = 
        ".check-box .box {" +
        "    -fx-background-color: white;" +
        "    -fx-border-color: #e2e8f0;" +
        "    -fx-border-width: 1.5;" +
        "    -fx-border-radius: 6;" +
        "    -fx-background-radius: 6;" +
        "    -fx-padding: 3;" +
        "}" +
        ".check-box:hover .box {" +
        "    -fx-border-color: #cbd5e1;" +
        "}" +
        ".check-box:selected .box {" +
        "    -fx-background-color: #4f46e5;" +
        "    -fx-border-color: #4f46e5;" +
        "}" +
        ".check-box:selected .mark {" +
        "    -fx-background-color: white;" +
        "    -fx-shape: \"M1.73,12.91L8.1,19.28L22.79,4.59L20.67,2.47L8.1,15.04L3.85,10.79L1.73,12.91Z\";" +
        "    -fx-scale-x: 0.7;" +
        "    -fx-scale-y: 0.7;" +
        "}" +
        ".check-box .label {" +
        "    -fx-text-fill: #1e293b;" +
        "    -fx-font-size: 13px;" +
        "    -fx-font-weight: 600;" +
        "    -fx-padding: 0 0 0 8;" +
        "}";
}
