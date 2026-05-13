package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.AcademicService;
import com.schoolbell.service.PdfParserService;
import com.schoolbell.service.ScheduleDataNormalizer;
import com.schoolbell.service.ScheduleDataNormalizer.ImportReport;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class ImportView {
    private static final Logger logger = LoggerFactory.getLogger(ImportView.class);
    private final MainApp mainApp;
    private final PdfParserService pdfParserService;
    private final ScheduleDataNormalizer normalizer;

    private StackPane container;
    private VBox mainContent;
    private VBox reviewContent;
    
    private ComboBox<String> sourceCombo;
    private ComboBox<String> formatCombo;
    
    private ImportReport lastReport;

    public ImportView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.pdfParserService = new PdfParserService();
        this.normalizer = new ScheduleDataNormalizer(mainApp.getAcademicService());
    }

    public Node build() {
        container = new StackPane();
        container.setStyle("-fx-background-color: " + COLOR_BG + ";");

        mainContent = buildMainContent();
        reviewContent = new VBox(25);
        reviewContent.setVisible(false);
        reviewContent.setManaged(false);

        container.getChildren().addAll(mainContent, reviewContent);
        
        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private VBox buildMainContent() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));

        VBox headerArea = createSectionHeader(
                "Імпорт даних",
                "Завантаження розкладу та довідників із зовнішніх джерел",
                COLOR_PURPLE,
                ICON_FOLDER
        );

        HBox settingsRow = new HBox(25);
        settingsRow.setAlignment(Pos.CENTER_LEFT);

        VBox sourceBox = new VBox(8);
        Label sourceLabel = new Label("ДЖЕРЕЛО ДАНИХ");
        sourceLabel.setStyle(HEADER_STYLE);
        sourceCombo = new ComboBox<>();
        sourceCombo.getItems().addAll("NZ.UA (Електронний журнал)", "Інше джерело");
        sourceCombo.setValue("NZ.UA (Електронний журнал)");
        sourceCombo.setStyle(COMBO_STYLE);
        sourceCombo.setPrefWidth(300);
        sourceBox.getChildren().addAll(sourceLabel, sourceCombo);

        VBox formatBox = new VBox(8);
        Label formatLabel = new Label("ФОРМАТ ФАЙЛУ");
        formatLabel.setStyle(HEADER_STYLE);
        formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("PDF файл", "JSON", "CSV", "TXT");
        formatCombo.setValue("PDF файл");
        formatCombo.setStyle(COMBO_STYLE);
        formatCombo.setPrefWidth(200);
        formatBox.getChildren().addAll(formatLabel, formatCombo);

        settingsRow.getChildren().addAll(sourceBox, formatBox);

        VBox dropZone = new VBox(20);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setPadding(new Insets(60));
        dropZone.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-border-color: #dfe6e9; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 24;");
        
        Node uploadIcon = createSVGIcon(ICON_FOLDER, Color.web(COLOR_PURPLE), 64);
        Label uploadTitle = new Label("Виберіть файл для імпорту");
        uploadTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        
        Button selectBtn = createPrimaryActionButton("АНАЛІЗУВАТИ ФАЙЛ", ICON_PLUS);
        selectBtn.setStyle(selectBtn.getStyle().replace(COLOR_PRIMARY, COLOR_PURPLE));
        
        selectBtn.setOnAction(e -> {
            String format = formatCombo.getValue();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Виберіть файл " + format);
            if ("PDF файл".equals(format)) fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            
            File selectedFile = fileChooser.showOpenDialog(mainApp.getStage());
            if (selectedFile != null) analyzeFile(selectedFile);
        });

        dropZone.getChildren().addAll(uploadIcon, uploadTitle, selectBtn);

        HBox helpRow = new HBox(25);
        helpRow.getChildren().addAll(
            createHelpCard(ICON_INFO, "Автоматична нормалізація", "Система автоматично розпізнає вчителів, предмети та кабінети.", COLOR_PURPLE),
            createHelpCard(ICON_SETTINGS, "Оновлення даних", "Якщо в розкладі з'явився новий предмет, система автоматично додасть цей зв'язок.", COLOR_SUCCESS)
        );

        root.getChildren().addAll(headerArea, settingsRow, dropZone, helpRow);
        return root;
    }

    private void analyzeFile(File file) {
        Task<ImportReport> task = new Task<>() {
            @Override
            protected ImportReport call() throws Exception {
                updateMessage("Читання PDF...");
                List<List<String>> rows = pdfParserService.extractTable(file);
                String fullText = pdfParserService.extractFullText(file);
                updateMessage("Аналіз структури...");
                return normalizer.analyzeImport(rows, fullText);
            }
        };

        task.setOnSucceeded(e -> {
            lastReport = task.getValue();
            showReviewStage(lastReport);
        });

        task.setOnFailed(e -> ToastService.showError("Помилка аналізу: " + task.getException().getMessage()));

        new Thread(task).start();
        ToastService.showInfo("Аналізуємо файл...");
    }

    private void showReviewStage(ImportReport report) {
        reviewContent.getChildren().clear();
        reviewContent.setPadding(new Insets(30));
        reviewContent.setSpacing(30);

        VBox headerArea = createSectionHeader(
                "Аналіз завершено",
                "Ми розпізнали структуру файлу. Перевірте оновлення перед імпортом.",
                COLOR_SUCCESS,
                ICON_CHECK
        );

        // Summary Cards Row
        HBox summaryRow = new HBox(20);
        summaryRow.getChildren().addAll(
                createReportCard("УРОКИ В ПЛАНІ", String.valueOf(report.totalLessons()), ICON_CALENDAR, COLOR_PRIMARY),
                createReportCard("ДЕНЬ / ТИЖДЕНЬ", "5 ДНІВ", ICON_CLOCK, COLOR_NEUTRAL)
        );

        VBox detailsBox = new VBox(20);
        Label detailsLabel = new Label("ЩО БУДЕ ДОДАНО ДО ДОВІДНИКІВ");
        detailsLabel.setStyle(HEADER_STYLE);
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        grid.add(createCategorySection("НОВІ ВЧИТЕЛІ", report.newTeachers(), ICON_PERSON, COLOR_PURPLE), 0, 0);
        grid.add(createCategorySection("НОВІ ПРЕДМЕТИ", report.newSubjects(), ICON_BOOK, COLOR_SUCCESS), 1, 0);
        grid.add(createCategorySection("НОВІ КЛАСИ", report.newClasses(), ICON_CLASS, COLOR_WARNING), 0, 1);
        grid.add(createCategorySection("НОВІ КАБІНЕТИ", report.newRooms(), ICON_ROOM, COLOR_DANGER), 1, 1);

        detailsBox.getChildren().addAll(detailsLabel, grid);

        if (report.newTeachers().isEmpty() && report.newSubjects().isEmpty() && 
            report.newClasses().isEmpty() && report.newRooms().isEmpty()) {
            
            VBox noChanges = new VBox(15);
            noChanges.setAlignment(Pos.CENTER);
            noChanges.setPadding(new Insets(40));
            noChanges.setStyle(SOFT_CARD + "-fx-background-color: #f8f9fa;");
            
            Node infoIcon = createSVGIcon(ICON_INFO, Color.web(COLOR_PRIMARY), 48);
            Label infoTitle = new Label("Всі дані вже існують в системі");
            infoTitle.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + "; -fx-font-size: 16px;");
            Label infoSub = new Label("Система оновить тільки розклад занять. Нових записів у довідниках не буде.");
            infoSub.setStyle("-fx-text-fill: " + COLOR_TEXT_DIM + ";");
            
            noChanges.getChildren().addAll(infoIcon, infoTitle, infoSub);
            detailsBox.getChildren().clear();
            detailsBox.getChildren().addAll(detailsLabel, noChanges);
        }

        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(20, 0, 0, 0));
        
        Button cancelBtn = new Button("СКАСУВАТИ");
        cancelBtn.setStyle(BTN_BASE + "-fx-background-color: #dfe6e9; -fx-text-fill: " + COLOR_TEXT + "; -fx-padding: 12 30;");
        cancelBtn.setOnAction(e -> hideReviewStage());

        Button applyBtn = createPrimaryActionButton("ПІДТВЕРДИТИ ТА ОНОВИТИ", ICON_CHECK);
        applyBtn.setOnAction(e -> executeImport());

        actions.getChildren().addAll(cancelBtn, applyBtn);

        reviewContent.getChildren().addAll(headerArea, summaryRow, detailsBox, actions);
        
        mainContent.setVisible(false);
        mainContent.setManaged(false);
        reviewContent.setVisible(true);
        reviewContent.setManaged(true);
    }

    private VBox createCategorySection(String title, java.util.Set<String> items, String icon, String color) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(20));
        section.setStyle(SOFT_CARD);
        VBox.setVgrow(section, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Node iconNode = createSVGIcon(icon, Color.web(color), 18);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 12px; -fx-text-fill: " + COLOR_TEXT + "; -fx-letter-spacing: 0.5px;");
        header.getChildren().addAll(iconNode, titleLabel);

        if (items.isEmpty()) {
            Label none = new Label("Нових елементів не знайдено");
            none.setStyle("-fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-size: 12px; -fx-font-style: italic;");
            section.getChildren().addAll(header, none);
            section.setOpacity(0.5);
        } else {
            FlowPane chips = new FlowPane(8, 8);
            for (String item : items) {
                Label chip = new Label(item);
                chip.setStyle("-fx-background-color: " + color + "10; -fx-text-fill: " + color + "; -fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 5 12; -fx-background-radius: 10; -fx-border-color: " + color + "30; -fx-border-radius: 10;");
                chips.getChildren().add(chip);
            }
            section.getChildren().addAll(header, chips);
        }
        return section;
    }

    private void hideReviewStage() {
        reviewContent.setVisible(false);
        reviewContent.setManaged(false);
        mainContent.setVisible(true);
        mainContent.setManaged(true);
    }

    private VBox createReportCard(String title, String value, String icon, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle(SOFT_CARD);
        card.setPrefWidth(300);
        
        HBox h = new HBox(15);
        h.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(color), 24));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(44, 44);
        iconBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 12;");
        
        VBox txt = new VBox(2);
        Label t = new Label(title); t.setStyle(HEADER_STYLE);
        Label v = new Label(value); v.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        txt.getChildren().addAll(t, v);
        
        h.getChildren().addAll(iconBox, txt);
        card.getChildren().add(h);
        return card;
    }

    private void executeImport() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Збереження в базу даних...");
                normalizer.executeImport(lastReport);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                mainApp.refreshCaches();
                ToastService.showSuccess("Дані успішно імпортовано!");
                hideReviewStage();
            });
        });

        task.setOnFailed(e -> ToastService.showError("Помилка імпорту: " + task.getException().getMessage()));

        new Thread(task).start();
    }
}
