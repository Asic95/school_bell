package com.schoolbell.ui;

import com.schoolbell.service.SignalService;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static com.schoolbell.ui.ControlFactory.createActionButton;
import static com.schoolbell.ui.ControlFactory.updateActionButton;
import static com.schoolbell.ui.UIStyles.*;

public class DashboardQuickActionsCard extends VBox {
    private final Button airRaidBtn;
    private final Button emergencyBtn;

    public DashboardQuickActionsCard(SignalService signalService) {
        super(20);
        Label headerLabel = new Label("ШВИДКІ ДІЇ");
        headerLabel.setStyle(HEADER_STYLE);

        HBox actionsBox = new HBox(20);
        airRaidBtn = createActionButton("ПОВІТРЯНА ТРИВОГА", "Активація режиму загальної небезпеки", ICON_AIR_RAID, GRADIENT_WARNING);
        airRaidBtn.setOnAction(e -> {
            airRaidBtn.setDisable(true);
            if ("AIR_RAID".equals(signalService.getCurrentAlertType())) {
                signalService.runAirRaidClearSignal();
            } else {
                signalService.runAirRaidSignal();
            }
        });
        HBox.setHgrow(airRaidBtn, Priority.ALWAYS);

        emergencyBtn = createActionButton("ЕКСТРЕНА СИТУАЦІЯ", "Сигнал термінової евакуації закладу", ICON_LIFEBUOY, GRADIENT_DANGER);
        emergencyBtn.setOnAction(e -> {
            emergencyBtn.setDisable(true);
            if ("EMERGENCY".equals(signalService.getCurrentAlertType())) {
                signalService.runEmergencyClearSignal();
            } else {
                signalService.runEmergencySignal();
            }
        });
        HBox.setHgrow(emergencyBtn, Priority.ALWAYS);

        actionsBox.getChildren().addAll(airRaidBtn, emergencyBtn);

        getChildren().addAll(headerLabel, actionsBox);
        setPadding(new Insets(25));
        setStyle(SOFT_CARD);
        setCache(true);
        setCacheHint(CacheHint.SPEED);
    }

    public void update(SignalService signalService) {
        String alert = signalService.getCurrentAlertType();
        boolean inProgress = signalService.isActionInProgress();

        if ("AIR_RAID".equals(alert)) {
            updateActionButton(airRaidBtn, "ВІДБІЙ ТРИВОГИ", "Сигнал про завершення небезпеки", ICON_ALL_CLEAR, GRADIENT_SUCCESS);
            emergencyBtn.setDisable(true);
            airRaidBtn.setDisable(inProgress);
        } else if ("EMERGENCY".equals(alert)) {
            updateActionButton(emergencyBtn, "СКАСУВАТИ НС", "Повернення до штатного режиму", ICON_ALL_CLEAR, GRADIENT_SUCCESS);
            airRaidBtn.setDisable(true);
            emergencyBtn.setDisable(inProgress);
        } else if ("SILENCE".equals(alert)) {
            updateActionButton(airRaidBtn, "ХВИЛИНА МОВЧАННЯ", "Йде відтворення аудіо", ICON_CLOCK, GRADIENT_NEUTRAL);
            airRaidBtn.setDisable(true);
            emergencyBtn.setDisable(true);
        } else {
            updateActionButton(airRaidBtn, "ПОВІТРЯНА ТРИВОГА", "Активація режиму загальної небезпеки", ICON_AIR_RAID, GRADIENT_WARNING);
            updateActionButton(emergencyBtn, "ЕКСТРЕНА СИТУАЦІЯ", "Сигнал термінової евакуації закладу", ICON_LIFEBUOY, GRADIENT_DANGER);
            airRaidBtn.setDisable(inProgress);
            emergencyBtn.setDisable(inProgress);
        }
    }
}
