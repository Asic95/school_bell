package com.schoolbell.ui;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import static com.schoolbell.ui.UIStyles.ICON_SCALE;

public class UIComponents {

    public static Node createSVGIcon(String pathData, Color color, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathData);
        svg.setFill(color);
        double scale = (size / 24.0) * ICON_SCALE;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return svg;
    }

    public static Node createSVGIcon(String pathData, String colorHex, double size) {
        return createSVGIcon(pathData, Color.web(colorHex), size);
    }
}
