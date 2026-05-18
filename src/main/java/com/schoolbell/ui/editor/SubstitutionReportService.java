package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.SchoolClass;
import com.schoolbell.model.Subject;
import com.schoolbell.model.SubstitutionEntry;
import com.schoolbell.model.Teacher;
import com.schoolbell.ui.ToastService;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SubstitutionReportService {
    private final MainApp mainApp;
    private final Locale ukLocale = Locale.of("uk", "UA");

    public SubstitutionReportService(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void generateReport(Month month, int year) {
        List<SubstitutionEntry> allSubs = mainApp.getAcademicService().getAllSubstitutions();
        List<SubstitutionEntry> filtered = allSubs.stream()
                .filter(s -> s.date().getMonth() == month && s.date().getYear() == year)
                .sorted((a, b) -> a.date().compareTo(b.date()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("За вказаний період замін не знайдено.");
            alert.show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Зберегти звіт");
        String mName = month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, ukLocale);
        mName = mName.substring(0, 1).toUpperCase() + mName.substring(1);
        fileChooser.setInitialFileName("звіт_замін_" + mName + "_" + year + ".txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        
        File file = fileChooser.showSaveDialog(mainApp.getStage());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
                writer.println("ЗВІТ ПРО ЗАМІНИ ВЧИТЕЛІВ");
                writer.println("Період: " + mName + " " + year);
                writer.println("Згенеровано: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                writer.println("==========================================");
                writer.println();

                // Group by teacher
                Map<String, List<SubstitutionEntry>> byTeacher = new TreeMap<>();
                for (SubstitutionEntry s : filtered) {
                    Teacher t = mainApp.getStaffService().getAllTeachers().stream().filter(tea -> tea.id() == s.teacherId()).findFirst().orElse(null);
                    String name = t != null ? t.name() : "Невідомий вчитель";
                    byTeacher.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(s);
                }

                for (Map.Entry<String, List<SubstitutionEntry>> entry : byTeacher.entrySet()) {
                    writer.println("ВЧИТЕЛЬ: " + entry.getKey());
                    writer.println("Кількість замін: " + entry.getValue().size());
                    writer.println("------------------------------------------");
                    for (SubstitutionEntry s : entry.getValue()) {
                        SchoolClass c = mainApp.getAcademicService().getAllClasses().stream().filter(cl -> cl.id() == s.classId()).findFirst().orElse(null);
                        Subject sub = mainApp.getStaffService().getAllSubjects().stream().filter(x -> x.id() == s.subjectId()).findFirst().orElse(null);
                        String roomName = s.classroomId() > 0 ? mainApp.getClassroomName(s.classroomId()) : "—";
                        writer.printf("  %s | Урок %d | Клас: %s | Предмет: %s | Каб: %s%n", 
                            s.date().format(DateTimeFormatter.ofPattern("dd.MM (EEE)", ukLocale)),
                            s.lessonNumber(),
                            (c != null ? c.name() : "?"),
                            (sub != null ? sub.name() : "Заміна"),
                            roomName
                        );
                    }
                    writer.println();
                }

                writer.println("==========================================");
                writer.println("Всього замін у звіті: " + filtered.size());

                ToastService.showSuccess("Звіт успішно збережено у файл: " + file.getName());
            } catch (Exception ex) {
                ToastService.showError("Помилка при збереженні файлу: " + ex.getMessage());
            }
        }
    }
}
