package com.schoolbell.ui;

import com.schoolbell.model.DaySchedule;
import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class DashboardTimeline {
    private final HBox container;
    private final List<Animation> activeAnimations = new ArrayList<>();

    public DashboardTimeline(HBox container) {
        this.container = container;
    }

    public void rebuild(DaySchedule ds, int curIdx, boolean isBreak, LocalTime now) {
        activeAnimations.forEach(Animation::stop);
        activeAnimations.clear();
        container.getChildren().clear();
        
        if (curIdx == -1) {
            if (now.isBefore(ds.getLessons().get(0).start)) {
                for (int i = 0; i < Math.min(3, ds.getLessons().size()); i++) {
                    addTimelinePoint(i + 1, ds.getLessons().get(i), "upcoming");
                }
            } else {
                int total = ds.getLessons().size();
                for (int i = Math.max(0, total - 3); i < total; i++) {
                    addTimelinePoint(i + 1, ds.getLessons().get(i), "completed");
                }
            }
        } else if (isBreak) {
            addTimelinePoint(curIdx + 1, ds.getLessons().get(curIdx), "completed");
            addTimelineBreakPoint();
            addTimelinePoint(curIdx + 2, ds.getLessons().get(curIdx + 1), "upcoming");
        } else {
            if (curIdx > 0) addTimelinePoint(curIdx, ds.getLessons().get(curIdx - 1), "completed");
            addTimelinePoint(curIdx + 1, ds.getLessons().get(curIdx), "active");
            if (curIdx < ds.getLessons().size() - 1) addTimelinePoint(curIdx + 2, ds.getLessons().get(curIdx + 1), "upcoming");
        }
    }

    private void addTimelinePoint(int number, DaySchedule.LessonInfo li, String status) {
        VBox node = new VBox(8);
        node.setAlignment(Pos.CENTER);
        node.setMinWidth(120);
        
        StackPane box = new StackPane();
        box.setPrefSize(44, 44);
        box.setCache(true);
        box.setCacheHint(CacheHint.SCALE);
        
        Label num = new Label(String.valueOf(number));
        num.setStyle("-fx-font-weight: 900; -fx-font-size: 15px;");
        
        if ("active".equals(status)) {
            box.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-background-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(9, 132, 227, 0.4), 12, 0, 0, 0);");
            num.setTextFill(Color.WHITE);
            
            ScaleTransition st = new ScaleTransition(Duration.millis(1000), box);
            st.setFromX(1.0); st.setFromY(1.0);
            st.setToX(1.1); st.setToY(1.1);
            st.setCycleCount(Animation.INDEFINITE);
            st.setAutoReverse(true);
            st.play();
            activeAnimations.add(st);
        } else if ("completed".equals(status)) {
            box.setStyle("-fx-background-color: " + COLOR_GREEN_LIGHT + "; -fx-background-radius: 14; -fx-border-color: " + COLOR_SUCCESS + "; -fx-border-width: 1.5; -fx-border-radius: 14;");
            num.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #16a085;"); 
            Node check = createSVGIcon(ICON_CHECK, Color.web(COLOR_SUCCESS), 14);
            StackPane.setAlignment(check, Pos.TOP_RIGHT);
            StackPane.setMargin(check, new Insets(5, 5, 0, 0));
            box.getChildren().add(check);
        } else {
            box.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #dfe6e9; -fx-border-width: 1.5; -fx-border-radius: 14; -fx-opacity: 0.8;");
            num.setTextFill(Color.web(COLOR_TEXT_DIM));
        }
        
        box.getChildren().add(num);
        
        Label time = new Label(li.start.toString());
        time.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        
        Label label = new Label("УРОК");
        label.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #b2bec3; -fx-letter-spacing: 1px;");
        
        node.getChildren().addAll(label, box, time);
        container.getChildren().add(node);
    }

    private void addTimelineBreakPoint() {
        VBox node = new VBox(8);
        node.setAlignment(Pos.CENTER);
        node.setMinWidth(120);
        
        StackPane box = new StackPane();
        box.setPrefSize(44, 44);
        box.setCache(true);
        box.setCacheHint(CacheHint.SCALE);
        box.setStyle("-fx-background-color: " + COLOR_WARNING + "; -fx-background-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(253, 203, 110, 0.3), 10, 0, 0, 0);");
        
        Node icon = createSVGIcon(ICON_CLOCK, Color.WHITE, 20);
        box.getChildren().add(icon);
        
        ScaleTransition st = new ScaleTransition(Duration.millis(1000), box);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.1); st.setToY(1.1);
        st.setCycleCount(Animation.INDEFINITE);
        st.setAutoReverse(true);
        st.play();
        activeAnimations.add(st);
        
        Label time = new Label("ПЕРЕРВА");
        time.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_WARNING + ";");
        
        Label label = new Label("ЗАРАЗ");
        label.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_WARNING + "; -fx-letter-spacing: 1px;");
        
        node.getChildren().addAll(label, box, time);
        container.getChildren().add(node);
    }

    public void clear() {
        container.getChildren().clear();
        activeAnimations.forEach(Animation::stop);
        activeAnimations.clear();
    }
}
