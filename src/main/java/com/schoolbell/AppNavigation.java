package com.schoolbell;

import com.schoolbell.ui.ScheduleEditorDialog;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
        mainApp.getHostServices().showDocument("https://github.com/Asic95/school_bell#school-bell--%D0%B0%D0%B2%D1%82%D0%BE%D0%BC%D0%B0%D1%82%D0%B8%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B0-%D1%81%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B0-%D1%88%D0%BA%D1%96%D0%BB%D1%8C%D0%BD%D0%B8%D1%85-%D0%B4%D0%B7%D0%B2%D1%96%D0%BD%D0%BA%D1%96%D0%B2");
    }

    public void createNavButton(String id, String text, String iconPath, Runnable action) {
        Button btn = new Button(text);
        btn.setGraphic(createSVGIcon(iconPath, Color.web(COLOR_ICON_MUTED), 20));
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
            if (v.getGraphic() instanceof javafx.scene.shape.SVGPath icon) icon.setFill(Color.web(COLOR_ICON_MUTED));
        });
        Button active = navButtons.get(id);
        if (active != null) {
            active.setStyle(NAV_BTN_BASE + NAV_BTN_ACTIVE);
            if (active.getGraphic() instanceof javafx.scene.shape.SVGPath icon) icon.setFill(Color.WHITE);
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
