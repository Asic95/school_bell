package com.schoolbell;

import com.schoolbell.ui.ScheduleEditorDialog;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
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

        Node oldNode = contentArea.getChildren().get(0);
        
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
        switchView(mainApp.getDashboardView().build());
    }

    public void showSchool() {
        setActiveNav("SCHOOL");
        switchView(mainApp.getSchoolView().build());
    }

    public void showSchedule() {
        setActiveNav("SCHEDULE");
        switchView(mainApp.getScheduleView().build());
    }

    public void showEditorTab(int tabIndex) {
        ScheduleEditorDialog editor = new ScheduleEditorDialog(mainApp);
        Node content = editor.createTabContent(tabIndex);
        switchView(content);
    }

    public void showEfir() { 
        setActiveNav("EFIR"); 
        switchView(mainApp.getEfirView().build()); 
    }

    public void showNotifications() {
        setActiveNav("NOTIFICATIONS");
        switchView(mainApp.getNotificationsView().build());
    }

    public void showSystem() {
        setActiveNav("SYSTEM");
        switchView(mainApp.getSystemView().build());
    }

    public void showImport() {
        setActiveNav("IMPORT");
        switchView(mainApp.getImportView().build());
    }
}
