package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.RadioStation;
import com.schoolbell.service.AudioService;
import com.schoolbell.service.RadioStationService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.Consumer;

import static com.schoolbell.ui.ControlFactory.createStyledField;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class RadioSelectionDialog extends BasePremiumDialog {
    private final RadioStationService radioService;
    private final AudioService previewAudioService;
    private final Consumer<RadioStation> onSelect;
    
    private FlowPane mainGrid;
    private TextField searchField;
    private Label sectionLabel;
    private Label counterLabel;
    
    private String currentlyPlayingUrl = null;
    private final java.util.Map<String, Image> imageCache = new java.util.HashMap<>();

    public RadioSelectionDialog(MainApp mainApp, Consumer<RadioStation> onSelect) {
        super(mainApp.getStage(), "Оберіть радіостанцію", "Онлайн Радіо", "Миттєвий вибір серед кращих станцій або пошук за назвою.", "");
        this.radioService = mainApp.getRadioStationService();
        this.previewAudioService = new AudioService(mainApp.getConfigService());
        this.onSelect = onSelect;

        // Ensure we stop preview when dialog closes
        this.setOnHidden(e -> previewAudioService.stopImmediate());

        // Hide save button
        if (saveBtn != null) {
            saveBtn.setVisible(false);
            saveBtn.setManaged(false);
        }

        VBox root = new VBox(25);
        root.setPadding(new Insets(10, 0, 0, 0));

        // --- SPOTLIGHT SEARCH ---
        VBox searchWrapper = new VBox(10);
        searchField = createStyledField("");
        searchField.setPromptText("Яку станцію шукаємо сьогодні?");
        searchField.setPrefHeight(60);
        searchField.setStyle(PREMIUM_FIELD_STYLE + "-fx-font-size: 18px; -fx-background-radius: 20; -fx-border-radius: 20; -fx-effect: dropshadow(three-pass-box, " + SHADOW_NAVY_08 + ", 20, 0, 0, 8);");
        searchField.textProperty().addListener((obs, old, newVal) -> updateDisplay(newVal));
        
        searchWrapper.getChildren().add(searchField);

        // --- DYNAMIC CONTENT SECTION ---
        VBox contentSection = new VBox(15);
        
        sectionLabel = new Label("ТОП-20 СТАНЦІЙ УКРАЇНИ");
        sectionLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-letter-spacing: 1.5px;");

        mainGrid = new FlowPane(20, 20);
        mainGrid.setPrefWrapLength(620);
        mainGrid.setAlignment(Pos.TOP_LEFT);
        mainGrid.setPadding(new Insets(10, 20, 20, 20));
        
        ScrollPane scroll = new ScrollPane(mainGrid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(480);
        scroll.setMinWidth(640);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        counterLabel = new Label();
        counterLabel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE + ";");

        contentSection.getChildren().addAll(sectionLabel, scroll, counterLabel);

        root.getChildren().addAll(searchWrapper, contentSection);
        content.getChildren().add(root);

        updateDisplay("");
        
        Platform.runLater(this::sizeToScene);
    }

    private void updateDisplay(String query) {
        mainGrid.getChildren().clear();
        boolean isSearch = query != null && !query.trim().isEmpty();
        
        if (isSearch) {
            sectionLabel.setText("РЕЗУЛЬТАТИ ПОШУКУ");
            sectionLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE + "; -fx-letter-spacing: 1.5px;");
        } else {
            sectionLabel.setText("ТОП-20 СТАНЦІЙ УКРАЇНИ");
            sectionLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-letter-spacing: 1.5px;");
        }

        List<RadioStationService.RadioBrowserStation> stations = radioService.searchCatalog(query);
        int limit = isSearch ? 40 : 20;
        int count = 0;
        
        for (RadioStationService.RadioBrowserStation bs : stations) {
            if (count >= limit) break;
            count++;
            
            RadioStation rs = new RadioStation(null, bs.name(), bs.url_resolved(), bs.favicon());
            mainGrid.getChildren().add(createStationCard(rs));
        }

        if (counterLabel != null) {
            if (isSearch) {
                counterLabel.setText("Знайдено " + stations.size() + " станцій.");
            } else {
                counterLabel.setText("Показано найпопулярніші хвилі. Всього в каталозі: " + radioService.getTotalCatalogSize());
            }
        }
    }

    private void refreshButtonStates() {
        for (javafx.scene.Node node : mainGrid.getChildren()) {
            if (node instanceof VBox card && card.getUserData() instanceof String url) {
                // Dig into the card structure to find the play button
                // Structure: Card (VBox) -> StackPane -> Play Button
                if (card.getChildren().get(0) instanceof StackPane iconStack) {
                    if (iconStack.getChildren().size() > 1 && iconStack.getChildren().get(1) instanceof Button playBtn) {
                        boolean isPlaying = url.equals(currentlyPlayingUrl);
                        String playIcon = isPlaying ? ICON_STOP : "M8,5.14V19.14L19,12.14L8,5.14Z";
                        playBtn.setGraphic(createSVGIcon(playIcon, Color.WHITE, 24));
                        playBtn.setOpacity(isPlaying ? 1.0 : (card.isHover() ? 1.0 : 0));
                    }
                }
            }
        }
    }

    private VBox createStationCard(RadioStation rs) {
        VBox card = new VBox(15);
        card.setUserData(rs.url()); // Link card to URL for refresh
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18));
        card.setPrefSize(170, 215);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 28; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 28; -fx-effect: dropshadow(three-pass-box, " + SHADOW_BLACK_03 + ", 10, 0, 0, 4);");

        // Icon Container with Play Overlay
        StackPane iconStack = new StackPane();
        iconStack.setPrefSize(64, 64);
        iconStack.setMaxSize(64, 64);
        
        VBox iconContainer = new VBox();
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setPrefSize(64, 64);
        iconContainer.setStyle("-fx-background-color: " + COLOR_SURFACE_SOFT + "; -fx-background-radius: 20;");
        
        ImageView iconView = new ImageView();
        if (rs.faviconUrl() != null && !rs.faviconUrl().isEmpty()) {
            Image img = imageCache.computeIfAbsent(rs.faviconUrl(), url -> new Image(url, 48, 48, true, true, true));
            if (img.isError()) {
                iconContainer.getChildren().add(createSVGIcon(ICON_RADIO, Color.web(COLOR_PRIMARY), 32));
            } else {
                iconView.setImage(img);
                iconContainer.getChildren().add(iconView);
            }
        } else {
            iconContainer.getChildren().add(createSVGIcon(ICON_RADIO, Color.web(COLOR_PRIMARY), 32));
        }

        // Play/Stop Overlay
        Button playBtn = new Button();
        boolean isPlaying = rs.url().equals(currentlyPlayingUrl);
        String playIcon = isPlaying ? ICON_STOP : "M8,5.14V19.14L19,12.14L8,5.14Z";
        playBtn.setGraphic(createSVGIcon(playIcon, Color.WHITE, 24));
        playBtn.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 20; -fx-cursor: hand;");
        playBtn.setPrefSize(64, 64);
        playBtn.setOpacity(isPlaying ? 1.0 : 0);
        
        // --- HOVER LOGIC ON CARD ---
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: " + COLOR_SURFACE_SKY + "; -fx-background-radius: 28; -fx-border-color: " + COLOR_PRIMARY + "; -fx-border-radius: 28; -fx-effect: dropshadow(three-pass-box, " + SHADOW_INDIGO_12 + ", 15, 0, 0, 8);");
            playBtn.setOpacity(1.0);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 28; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 28; -fx-effect: dropshadow(three-pass-box, " + SHADOW_BLACK_03 + ", 10, 0, 0, 4);");
            if (!rs.url().equals(currentlyPlayingUrl)) playBtn.setOpacity(0);
        });

        playBtn.setOnAction(e -> {
            if (rs.url().equals(currentlyPlayingUrl)) {
                previewAudioService.stopImmediate();
                currentlyPlayingUrl = null;
            } else {
                previewAudioService.playAudioFile(rs.url(), rs.name());
                currentlyPlayingUrl = rs.url();
            }
            refreshButtonStates(); // NO FLICKER: only update icons
        });

        iconStack.getChildren().addAll(iconContainer, playBtn);

        Label name = new Label(rs.name());
        name.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: " + COLOR_NAVY + "; -fx-text-alignment: center;");
        name.setWrapText(true);
        name.setMaxWidth(150);
        name.setMinHeight(45);
        name.setAlignment(Pos.CENTER);
        VBox.setVgrow(name, Priority.ALWAYS);

        Button selectBtn = new Button("ОБРАТИ");
        selectBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 8 25; -fx-background-radius: 12; -fx-cursor: hand;");
        selectBtn.setOpacity(0.9);
        selectBtn.setMinWidth(120);
        selectBtn.setOnAction(e -> {
            onSelect.accept(rs);
            close();
        });

        card.getChildren().addAll(iconStack, name, selectBtn);
        
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                onSelect.accept(rs);
                close();
            }
        });

        return card;
    }

    @Override protected boolean onSave() { return true; }
}
