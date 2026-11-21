package com.schoolbell;

import com.schoolbell.ui.ScheduleEditorDialog;
import com.schoolbell.ui.TelegramGroupDialog;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class AppNavigation {
    private final MainApp mainApp;
    private final VBox sidebar;
    private final StackPane contentArea;
    private final Map<String, Button> navButtons = new HashMap<>();

    // Keep the main pages alive and reuse their node trees instead of rebuilding
    // the whole JavaFX hierarchy on every navigation click.
    private Node dashboardNode;
    private Node scheduleNode;
    private Node notificationsNode;
    private Node efirNode;
    private Node schoolNode;
    private Node importNode;
    private Node systemNode;

    public AppNavigation(MainApp mainApp, VBox sidebar, StackPane contentArea) {
        this.mainApp = mainApp;
        this.sidebar = sidebar;
        this.contentArea = contentArea;
    }

    public void init() {
        createNavButton("DASHBOARD", "Головна", ICON_DASHBOARD, this::showDashboard);
        createNavButton("SCHEDULE", "Розклад", ICON_CALENDAR, this::showSchedule);
        createNavButton("NOTIFICATIONS", "Сповіщення", ICON_NOTIFICATIONS, this::showNotifications);
        createNavButton("EFIR", "Ефір", ICON_BROADCAST, this::showEfir);
        createNavButton("SCHOOL", "Школа", ICON_FOLDER, this::showSchool);
        createNavButton("IMPORT", "Імпорт", ICON_PLUS, this::showImport);
        createNavButton("SYSTEM", "Система", ICON_SETTINGS, this::showSystem);
        // External Help Link
        createNavButton("HELP", "Допомога", ICON_INFO, this::openHelp);
    }

    private void openHelp() {
        new TelegramGroupDialog(mainApp.getStage(), mainApp).display();
    }

    public void createNavButton(String id, String text, String iconPath, Runnable action) {
        Button btn = new Button(text);
        Node icon = createSVGIcon(iconPath, Color.web(COLOR_ICON_MUTED), 20);
        if ("HELP".equals(id)) {
            StackPane iconWithBadge = new StackPane(icon);
            Label badge = new Label("NEW");
            badge.setStyle("-fx-background-color: #229ED9; -fx-text-fill: white; -fx-font-size: 8px; -fx-font-weight: 800; -fx-padding: 2 3 2 3; -fx-background-radius: 5;");
            StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_RIGHT);
            badge.setTranslateX(5);
            badge.setTranslateY(-5);
            iconWithBadge.getChildren().add(badge);
            btn.setGraphic(iconWithBadge);
            Tooltip.install(btn, new Tooltip("Група SchoolBell у Telegram"));
        } else {
            btn.setGraphic(icon);
        }
        btn.setGraphicTextGap(15);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(NAV_BTN_BASE);
        VBox.setMargin(btn, new Insets(2, 15, 2, 15));
        btn.setOnAction(e -> {
            setActiveNav(id);
            action.run();
        });
        btn.setOnMouseEntered(e -> { if (!btn.getStyle().contains(NAV_BTN_ACTIVE)) btn.setStyle(NAV_BTN_BASE + NAV_BTN_HOVER); });
        btn.setOnMouseExited(e -> { if (!btn.getStyle().contains(NAV_BTN_ACTIVE)) btn.setStyle(NAV_BTN_BASE); });
        sidebar.getChildren().add(btn);
        navButtons.put(id, btn);
    }

    public void setActiveNav(String id) {
        navButtons.forEach((k, v) -> {
            v.setStyle(NAV_BTN_BASE);
            setNavIconColor(v.getGraphic(), Color.web(COLOR_ICON_MUTED));
        });
        Button active = navButtons.get(id);
        if (active != null) {
            active.setStyle(NAV_BTN_BASE + NAV_BTN_ACTIVE);
            setNavIconColor(active.getGraphic(), Color.WHITE);
        }
    }

    private void setNavIconColor(Node node, Color color) {
        if (node instanceof javafx.scene.shape.SVGPath icon) {
            icon.setFill(color);
        } else if (node instanceof javafx.scene.Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> setNavIconColor(child, color));
        }
    }

    private void switchView(Node newNode) {
        if (contentArea.getChildren().isEmpty()) {
            contentArea.getChildren().setAll(newNode);
            return;
        }

        Node oldNode = contentArea.getChildren().getFirst();
        
        // Skip transition if it's the same node type/instance
        if (oldNode == newNode) return;

        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(120), oldNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            newNode.setOpacity(0.0);
            contentArea.getChildren().setAll(newNode);
            
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), newNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    public void showDashboard() {
        setActiveNav("DASHBOARD");
        if (dashboardNode == null) dashboardNode = mainApp.getDashboardView().build();
        switchView(dashboardNode);
    }

    public void showSchool() {
        setActiveNav("SCHOOL");
        if (schoolNode == null) schoolNode = mainApp.getSchoolView().build();
        switchView(schoolNode);
    }

    public void showSchedule() {
        setActiveNav("SCHEDULE");
        if (scheduleNode == null) scheduleNode = mainApp.getScheduleView().build();
        switchView(scheduleNode);
    }

    public void showEditorTab(int tabIndex) {
        ScheduleEditorDialog editor = new ScheduleEditorDialog(mainApp);
        Node content = editor.createTabContent(tabIndex);
        switchView(content);
    }

    public void showEfir() { 
        setActiveNav("EFIR"); 
        if (efirNode == null) efirNode = mainApp.getEfirView().build();
        else mainApp.getEfirView().refreshAll();
        switchView(efirNode); 
    }

    public void showNotifications() {
        setActiveNav("NOTIFICATIONS");
        if (notificationsNode == null) notificationsNode = mainApp.getNotificationsView().build();
        switchView(notificationsNode);
    }

    public void showSystem() {
        setActiveNav("SYSTEM");
        if (systemNode == null) systemNode = mainApp.getSystemView().build();
        switchView(systemNode);
    }

    public void showImport() {
        setActiveNav("IMPORT");
        if (importNode == null) importNode = mainApp.getImportView().build();
        switchView(importNode);
    }
}
