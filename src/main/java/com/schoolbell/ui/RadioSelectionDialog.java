package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.RadioStation;
import com.schoolbell.service.RadioStationService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.Consumer;

import static com.schoolbell.ui.ControlFactory.createStyledField;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class RadioSelectionDialog extends BasePremiumDialog {
    private final RadioStationService radioService;
    private final Consumer<RadioStation> onSelect;
    
    private VBox favoritesList;
    private VBox catalogList;
    private TextField searchField;
    private Label counterLabel;

    public RadioSelectionDialog(MainApp mainApp, Consumer<RadioStation> onSelect) {
        super(mainApp.getStage(), "Оберіть радіостанцію", "Онлайн Радіо", "Оберіть станцію зі списку або знайдіть нову в каталозі.", "");
        this.radioService = mainApp.getRadioStationService();
        this.onSelect = onSelect;

        // Customize footer: Hide save button text, use only icon
        if (saveBtn != null) {
            saveBtn.setText("");
            saveBtn.setGraphic(createSVGIcon(ICON_SAVE, Color.WHITE, 20));
            saveBtn.setMinWidth(60);
            saveBtn.setMaxWidth(60);
            saveBtn.setPrefWidth(60);
        }

        VBox root = new VBox(20);
        root.setPadding(new Insets(10, 0, 0, 0));

        // --- FAVORITES SECTION ---
        VBox favSection = new VBox(10);
        Label favLabel = new Label("ВАШІ СТАНЦІЇ");
        favLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-letter-spacing: 1.5px;");
        
        favoritesList = new VBox(8);
        favoritesList.setPadding(new Insets(0, 12, 0, 0)); // Space for scrollbar
        ScrollPane favScroll = new ScrollPane(favoritesList);
        favScroll.setFitToWidth(true);
        favScroll.setPrefHeight(235); // Exactly ~3 items (72px per item + 8px gap)
        favScroll.setMinHeight(235);
        favScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        favSection.getChildren().addAll(favLabel, favScroll);

        // --- CATALOG SECTION ---
        VBox catSection = new VBox(15);
        Label catLabel = new Label("КАТАЛОГ УКРАЇНИ");
        catLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-letter-spacing: 1.5px;");

        searchField = createStyledField("");
        searchField.setPromptText("Пошук станції за назвою...");
        searchField.textProperty().addListener((obs, old, newVal) -> updateCatalog(newVal));

        HBox catHeader = new HBox(15, catLabel);
        catHeader.setAlignment(Pos.CENTER_LEFT);

        catalogList = new VBox(8);
        catalogList.setPadding(new Insets(0, 12, 0, 0)); // Space for scrollbar
        ScrollPane catScroll = new ScrollPane(catalogList);
        catScroll.setFitToWidth(true);
        catScroll.setPrefHeight(235); // Exactly ~3 items
        catScroll.setMinHeight(235);
        catScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        counterLabel = new Label();
        counterLabel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 15px; -fx-text-fill: " + COLOR_SLATE + "; -fx-padding: 5 0;");

        catSection.getChildren().addAll(catHeader, searchField, catScroll, counterLabel);

        root.getChildren().addAll(favSection, new Separator(), catSection);
        content.getChildren().add(root);

        refreshFavorites();
        updateCatalog("");
        
        // Final window sizing
        Platform.runLater(this::sizeToScene);
    }

    private void refreshFavorites() {
        favoritesList.getChildren().clear();
        List<RadioStation> favorites = radioService.getFavorites();
        
        if (favorites.isEmpty()) {
            Label empty = new Label("Список порожній. Додайте станції з каталогу нижче.");
            empty.setStyle("-fx-text-fill: " + COLOR_SLATE + "; -fx-font-style: italic; -fx-padding: 10;");
            favoritesList.getChildren().add(empty);
        } else {
            for (RadioStation rs : favorites) {
                favoritesList.getChildren().add(createStationRow(rs, true));
            }
        }
    }

    private void updateCatalog(String query) {
        catalogList.getChildren().clear();
        List<RadioStationService.RadioBrowserStation> results = radioService.searchCatalog(query);
        
        int limit = 20;
        int count = 0;
        for (RadioStationService.RadioBrowserStation bs : results) {
            if (count >= limit) break;
            count++;
            
            // Convert BrowserStation to regular RadioStation for UI reuse
            RadioStation rs = new RadioStation(null, bs.name(), bs.url_resolved(), bs.favicon());
            catalogList.getChildren().add(createStationRow(rs, false));
        }

        if (counterLabel != null) {
            int total = radioService.getTotalCatalogSize();
            if (query == null || query.isEmpty()) {
                counterLabel.setText("Показано ТОП-20 станцій. Всього в каталозі: " + total);
            } else {
                counterLabel.setText("Знайдено " + results.size() + " станцій (показано перші " + Math.min(results.size(), limit) + ")");
            }
        }
    }

    private HBox createStationRow(RadioStation rs, boolean isFavorite) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 18, 12, 18));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 16;");

        VBox iconBox = new VBox(createSVGIcon(ICON_RADIO, Color.web(COLOR_PRIMARY), 22));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(44, 44);
        iconBox.setStyle("-fx-background-color: " + COLOR_SURFACE_SKY + "; -fx-background-radius: 12;");

        VBox text = new VBox(2);
        Label name = new Label(rs.name());
        name.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: " + COLOR_NAVY + ";");
        Label url = new Label(rs.url());
        url.setStyle("-fx-font-size: 10px; -fx-text-fill: " + COLOR_SLATE + ";");
        text.getChildren().addAll(name, url);
        HBox.setHgrow(text, Priority.ALWAYS);

        if (isFavorite) {
            Button actionBtn = new Button("ОБРАТИ");
            actionBtn.setGraphic(createSVGIcon(ICON_CHECK, Color.WHITE, 14));
            actionBtn.setStyle(PREMIUM_BTN_STYLE + "-fx-font-size: 10px; -fx-padding: 8 16; -fx-background-radius: 12;");
            actionBtn.setOnAction(e -> {
                onSelect.accept(rs);
                close();
            });
            
            Button deleteBtn = new Button();
            deleteBtn.setGraphic(createSVGIcon(ICON_TRASH, Color.web(COLOR_DANGER), 18));
            String clearStyle = "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: " + COLOR_DANGER_BORDER + "; -fx-border-radius: 14; -fx-padding: 10; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, " + SHADOW_BLACK_03 + ", 5, 0, 0, 1);";
            deleteBtn.setStyle(clearStyle);
            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(clearStyle + "-fx-background-color: " + COLOR_DANGER_PALE + "; -fx-border-color: " + COLOR_DANGER + ";"));
            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(clearStyle));
            deleteBtn.setOnAction(e -> {
                radioService.removeFromFavorites(rs.id());
                refreshFavorites();
            });
            
            row.getChildren().addAll(iconBox, text, actionBtn, deleteBtn);
        } else {
            Button addBtn = new Button("ДОДАТИ");
            addBtn.setGraphic(createSVGIcon(ICON_PLUS, Color.web(COLOR_PRIMARY), 14));
            addBtn.setStyle("-fx-background-color: white; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 8 16; -fx-background-radius: 12; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 12; -fx-cursor: hand;");
            addBtn.setOnAction(e -> {
                radioService.addToFavorites(new RadioStationService.RadioBrowserStation(rs.name(), rs.url(), "MP3", null, 1));
                refreshFavorites();
                ToastService.showSuccess("Станцію додано до ваших списків");
            });
            row.getChildren().addAll(iconBox, text, addBtn);
        }

        return row;
    }

    @Override protected boolean onSave() { return true; }
}
