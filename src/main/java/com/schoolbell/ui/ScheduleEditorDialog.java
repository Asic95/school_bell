package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.ui.editor.AnnouncementsEditorTab;
import com.schoolbell.ui.editor.BellsEditorTab;
import com.schoolbell.ui.editor.ClassesEditorTab;
import com.schoolbell.ui.editor.ClassroomsEditorTab;
import com.schoolbell.ui.editor.SubjectsEditorTab;
import com.schoolbell.ui.editor.SubstitutionsEditorTab;
import com.schoolbell.ui.editor.TeachersEditorTab;
import com.schoolbell.ui.editor.WeeklyScheduleEditorTab;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class ScheduleEditorDialog {
    private final MainApp mainApp;

    private final BellsEditorTab bellsTab;
    private final TeachersEditorTab teachersTab;
    private final SubjectsEditorTab subjectsTab;
    private final ClassesEditorTab classesTab;
    private final WeeklyScheduleEditorTab weeklyTab;
    private final SubstitutionsEditorTab substitutionsTab;
    private final AnnouncementsEditorTab announcementsTab;
    private final ClassroomsEditorTab classroomsTab;

    public ScheduleEditorDialog(MainApp mainApp) {
        this.mainApp = mainApp;
        this.bellsTab = new BellsEditorTab(mainApp);
        this.teachersTab = new TeachersEditorTab(mainApp);
        this.subjectsTab = new SubjectsEditorTab(mainApp);
        this.classesTab = new ClassesEditorTab(mainApp);
        this.weeklyTab = new WeeklyScheduleEditorTab(mainApp, this);
        this.substitutionsTab = new SubstitutionsEditorTab(mainApp, this);
        this.announcementsTab = new AnnouncementsEditorTab(mainApp);
        this.classroomsTab = new ClassroomsEditorTab(mainApp);
    }

    public Node createTabContent(int index) {
        return switch (index) {
            case 0 -> bellsTab.createContent();
            case 1 -> teachersTab.createContent();
            case 2 -> subjectsTab.createContent();
            case 3 -> classesTab.createContent();
            case 4 -> weeklyTab.createContent();
            case 5 -> substitutionsTab.createContent();
            case 6 -> announcementsTab.createContent();
            case 7 -> classroomsTab.createContent();
            default -> new Label("Unknown Tab");
        };
    }
}
