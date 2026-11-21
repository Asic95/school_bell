package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import static com.schoolbell.ui.UIStyles.COLOR_BORDER_SOFT;
import static com.schoolbell.ui.UIStyles.COLOR_NAVY;
import static com.schoolbell.ui.UIStyles.COLOR_SLATE;
import static com.schoolbell.ui.UIStyles.ICON_LINK;

/** Shared support and project resources dialog for SchoolBell. */
public class TelegramGroupDialog extends BasePremiumDialog {

    private static final String QR_RESOURCE = "/tg_group_qr_code.svg";
    private static final String GITHUB_URL = "https://github.com/Asic95/school_bell";
    private final MainApp mainApp;

    public TelegramGroupDialog(Stage owner, MainApp mainApp) {
        super(owner,
                "ПІДТРИМКА SCHOOLBELL",
                "Допомога та спільнота",
                "Спілкуйтеся з користувачами в Telegram або переглядайте відкриті матеріали проєкту на GitHub.",
                "ЗАКРИТИ",
                760);
        this.mainApp = mainApp;

        root.setSpacing(18);
        content.setSpacing(0);

        HBox columns = new HBox(28);
        columns.setAlignment(Pos.CENTER);

        VBox telegramColumn = new VBox(12);
        telegramColumn.setAlignment(Pos.CENTER);
        telegramColumn.setPrefWidth(320);
        Label telegramTitle = new Label("Спілкування в Telegram");
        telegramTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_NAVY + ";");
        Label telegramHint = new Label("Це основний канал для запитань, допомоги та обговорення роботи SchoolBell. Скануйте QR-код, щоб приєднатися до групи.");
        telegramHint.setWrapText(true);
        telegramHint.setAlignment(Pos.CENTER);
        telegramHint.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE + ";");

        VBox qrCard = new VBox();
        qrCard.setAlignment(Pos.CENTER);
        qrCard.setPadding(new Insets(12));
        qrCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + COLOR_BORDER_SOFT + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 14;"
        );

        WebView qrView = createQrView();
        qrCard.getChildren().add(qrView);
        telegramColumn.getChildren().addAll(telegramTitle, telegramHint, qrCard);

        Region divider = new Region();
        divider.setPrefWidth(1);
        divider.setMinWidth(1);
        divider.setPrefHeight(300);
        divider.setStyle("-fx-background-color: " + COLOR_BORDER_SOFT + ";");

        VBox projectColumn = new VBox(14);
        projectColumn.setAlignment(Pos.TOP_LEFT);
        projectColumn.setPrefWidth(320);
        Label projectTitle = new Label("Відкритий проєкт на GitHub");
        projectTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_NAVY + ";");
        Label projectDescription = new Label("Тут доступні відкритий код, документація, інструкції, файли для завантаження та інформація про оновлення проєкту.");
        projectDescription.setWrapText(true);
        projectDescription.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE + ";");

        VBox githubSection = new VBox(10);
        githubSection.setPadding(new Insets(16));
        githubSection.setStyle(
                "-fx-background-color: #f8fafc;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + COLOR_BORDER_SOFT + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 14;"
        );
        VBox githubText = new VBox(3);
        Label githubTitle = new Label("GitHub проєкту");
        githubTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_NAVY + ";");
        Label githubHint = new Label("Вихідний код, інструкції та файли");
        githubHint.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_SLATE + ";");
        githubText.getChildren().addAll(githubTitle, githubHint);

        Button githubButton = ControlFactory.createSmallPrimaryActionButton("ВІДКРИТИ", ICON_LINK);
        githubButton.setOnAction(event -> mainApp.getHostServices().showDocument(GITHUB_URL));
        githubSection.getChildren().addAll(githubText, githubButton);

        VBox supportHint = new VBox(6);
        supportHint.setPadding(new Insets(14, 16, 14, 16));
        supportHint.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 14;");
        Label supportTitle = new Label("Куди звертатися?");
        supportTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_NAVY + ";");
        Label supportText = new Label("Для спілкування та швидкої допомоги використовуйте Telegram, а на GitHub переглядайте інструкції та матеріали проєкту");
        supportText.setWrapText(true);
        supportText.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLOR_SLATE + ";");
        supportHint.getChildren().addAll(supportTitle, supportText);

        projectColumn.getChildren().addAll(projectTitle, projectDescription, githubSection, supportHint);
        columns.getChildren().addAll(telegramColumn, divider, projectColumn);

        content.getChildren().add(columns);
        actions.getChildren().remove(saveBtn);
        HBox.setHgrow(cancelBtn, Priority.NEVER);
        actions.setAlignment(Pos.CENTER);
        cancelBtn.setText("ЗАКРИТИ");
    }

    private WebView createQrView() {
        WebView view = new WebView();
        view.setPrefSize(240, 240);
        view.setMinSize(240, 240);
        view.setMaxSize(240, 240);
        view.setContextMenuEnabled(false);
        view.getEngine().setJavaScriptEnabled(false);

        var resource = getClass().getResource(QR_RESOURCE);
        if (resource != null) {
            view.getEngine().load(resource.toExternalForm());
        } else {
            view.getEngine().loadContent("<html><body style='font-family:sans-serif;text-align:center'>QR-код недоступний</body></html>");
        }
        return view;
    }

    @Override
    protected boolean onSave() {
        return true;
    }
}
