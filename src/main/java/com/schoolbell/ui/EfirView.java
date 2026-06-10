package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static com.schoolbell.ui.ControlFactory.createPageHeader;
import static com.schoolbell.ui.UIStyles.*;

public class EfirView {
    private final MainApp mainApp;
    private final ConfigService config;

    private EfirStatusCard statusCard;
    private EfirAnnouncementsSection announcementsSection;
    private EfirSideSection sideSection;
    private EfirNetworkSection networkSection;

    public EfirView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
    }

    public Node build() {
        VBox root = new VBox(28);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        HBox header = createPageHeader(
            "ЦЕНТР КЕРУВАННЯ ЕФІРОМ",
            "Керування ефіром",
            "Контроль трансляції, планування оголошень та моніторинг підключених пристроїв.",
            ICON_BROADCAST,
            COLOR_INDIGO,
            null
        );

        statusCard = new EfirStatusCard(mainApp);

        HBox mainContent = new HBox(28);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        announcementsSection = new EfirAnnouncementsSection(mainApp);
        HBox.setHgrow(announcementsSection, Priority.ALWAYS);
        announcementsSection.setMinWidth(400);

        sideSection = new EfirSideSection(mainApp);
        sideSection.setPrefWidth(480); 
        sideSection.setMinWidth(480);
        sideSection.setMaxWidth(480);

        mainContent.getChildren().addAll(announcementsSection, sideSection);

        networkSection = new EfirNetworkSection(mainApp, statusCard);

        root.getChildren().addAll(header, statusCard, mainContent, networkSection);

        refreshAll();

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }

    public void refreshAll() {
        if (announcementsSection != null) announcementsSection.refreshAnnouncements();
        if (sideSection != null) sideSection.refreshDevices();
        if (networkSection != null) networkSection.updateFirewallStatusLabel();
    }
}
