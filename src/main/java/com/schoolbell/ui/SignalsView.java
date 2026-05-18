package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.scene.Node;

public class SignalsView {
    @SuppressWarnings("unused")
    private final MainApp mainApp;

    public SignalsView(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node build() {
        return new SignalSettingsPane();
    }
}