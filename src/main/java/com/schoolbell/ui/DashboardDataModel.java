package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.model.SchoolClass;
import com.schoolbell.model.SubstitutionEntry;
import com.schoolbell.service.AcademicService;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.SignalService;

import java.time.LocalTime;
import java.util.*;

public class DashboardDataModel {
    private final MainApp mainApp;
    private final ConfigService config;
    private final SignalService signalService;
    private final AcademicService academicService;

    public DashboardDataModel(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.signalService = mainApp.getSignalService();
        this.academicService = mainApp.getAcademicService();
    }

    public Map<String, Object> getExtendedDashboardData(LocalTime now) {
        Map<String, Object> data = new HashMap<>();
        data.put("schoolName", config.getSchoolName());
        data.put("cityName", config.getCityName());
        
        String alert = signalService.getCurrentAlertType();
        if ("AIR_RAID".equals(alert) && !config.isVisualAirRaidEnabled()) alert = "NONE";
        if ("EMERGENCY".equals(alert) && !config.isVisualEmergencyEnabled()) alert = "NONE";
        if ("SILENCE".equals(alert) && !config.isVisualSilenceEnabled()) alert = "NONE";
        data.put("alertType", alert);

        data.put("countdown", calculateCountdown(now));
        data.put("scheduleName", config.getSelectedScheduleName());

        DaySchedule activeDs = mainApp.getInternalSchedules().stream()
                .filter(ds -> ds.getName().equals(config.getSelectedScheduleName()))
                .findFirst().orElse(null);
        
        if (activeDs == null) return data;

        List<Map<String, Object>> stages = new ArrayList<>();
        int currentStageIndex = -1;
        for (int i = 0; i < activeDs.getLessons().size(); i++) {
            DaySchedule.LessonInfo li = activeDs.getLessons().get(i);
            Map<String, Object> lessonStage = new HashMap<>();
            lessonStage.put("type", "LESSON");
            lessonStage.put("number", i + 1);
            lessonStage.put("title", (i + 1) + " урок");
            lessonStage.put("start", li.start != null ? li.start.toString() : "--:--");
            lessonStage.put("end", li.end != null ? li.end.toString() : "--:--");
            
            if (li.start != null && li.end != null) {
                if (now.isBefore(li.start)) {
                    lessonStage.put("status", "upcoming");
                } else if (now.isAfter(li.end)) {
                    lessonStage.put("status", "completed");
                } else {
                    lessonStage.put("status", "active");
                    currentStageIndex = stages.size();
                    long total = java.time.Duration.between(li.start, li.end).toSeconds();
                    long elapsed = java.time.Duration.between(li.start, now).toSeconds();
                    lessonStage.put("progress", (elapsed * 100.0) / total);
                }
            } else {
                lessonStage.put("status", "upcoming");
            }
            stages.add(lessonStage);

            if (i < activeDs.getLessons().size() - 1) {
                DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(i + 1);
                if (li.end != null && nextLi.start != null) {
                    Map<String, Object> breakStage = new HashMap<>();
                    breakStage.put("type", "BREAK");
                    breakStage.put("title", "Перерва");
                    breakStage.put("start", li.end.toString());
                    breakStage.put("end", nextLi.start.toString());
                    
                    if (now.isBefore(li.end)) {
                        breakStage.put("status", "upcoming");
                    } else if (now.isAfter(nextLi.start)) {
                        breakStage.put("status", "completed");
                    } else {
                        breakStage.put("status", "active");
                        currentStageIndex = stages.size();
                        long total = java.time.Duration.between(li.end, nextLi.start).toSeconds();
                        long elapsed = java.time.Duration.between(li.end, now).toSeconds();
                        breakStage.put("progress", (elapsed * 100.0) / total);
                    }
                    stages.add(breakStage);
                }
            }
        }
        data.put("stages", stages);
        data.put("currentStageIndex", currentStageIndex);
        if (currentStageIndex != -1) {
            data.put("schoolStatus", stages.get(currentStageIndex));
        }

        List<Map<String, Object>> classStatuses = new ArrayList<>();
        int currentLessonNum = -1;
        boolean isBreak = false;
        
        for (int i = 0; i < activeDs.getLessons().size(); i++) {
            DaySchedule.LessonInfo li = activeDs.getLessons().get(i);
            if (li.start != null && li.end != null && !now.isBefore(li.start) && !now.isAfter(li.end)) {
                currentLessonNum = i + 1;
                break;
            }
        }

        if (currentLessonNum == -1) {
            for (int i = 0; i < activeDs.getLessons().size(); i++) {
                DaySchedule.LessonInfo li = activeDs.getLessons().get(i);
                if (li.start != null && now.isBefore(li.start)) {
                    currentLessonNum = i + 1;
                    isBreak = true;
                    break;
                }
            }
        }

        if (currentLessonNum != -1) {
            java.time.LocalDate today = java.time.LocalDate.now();
            int dayOfWeek = today.getDayOfWeek().getValue();
            List<SubstitutionEntry> subs = academicService.getSubstitutionsForDate(today);
            
            for (SchoolClass sc : mainApp.getClassCache()) {
                List<com.schoolbell.model.ScheduleEntry> baseSched = academicService.getScheduleForClass(sc.id());
                
                int targetLNum = -1;
                boolean foundReplacement = false;
                SubstitutionEntry targetSub = null;
                com.schoolbell.model.ScheduleEntry targetBase = null;

                for (int l = currentLessonNum; l <= 15; l++) {
                    final int checkL = l;
                    SubstitutionEntry sub = subs.stream()
                            .filter(s -> s.classId() == sc.id() && s.lessonNumber() == checkL)
                            .findFirst().orElse(null);
                    
                    if (sub != null) {
                        targetLNum = l;
                        targetSub = sub;
                        foundReplacement = true;
                        break;
                    }

                    com.schoolbell.model.ScheduleEntry base = baseSched.stream()
                            .filter(e -> e.dayOfWeek() == dayOfWeek && e.lessonNumber() == checkL)
                            .findFirst().orElse(null);
                    
                    if (base != null) {
                        targetLNum = l;
                        targetBase = base;
                        break;
                    }
                }

                if (targetLNum == -1) continue;

                Map<String, Object> cs = new HashMap<>();
                cs.put("className", sc.name());
                cs.put("lessonNumber", targetLNum);
                
                boolean isClassInCurrentSchoolLesson = (!isBreak && targetLNum == currentLessonNum);
                cs.put("statusClass", isClassInCurrentSchoolLesson ? "current" : "upcoming");

                if (foundReplacement && targetSub != null) {
                    cs.put("isReplacement", true);
                    cs.put("subject", mainApp.getSubjectName(targetSub.subjectId()));
                    cs.put("teacher", mainApp.getTeacherName(targetSub.teacherId()));
                    cs.put("room", mainApp.getClassroomName(targetSub.classroomId()));
                    
                    final int finalTargetLNum = targetLNum;
                    baseSched.stream()
                        .filter(e -> e.dayOfWeek() == dayOfWeek && e.lessonNumber() == finalTargetLNum)
                        .findFirst()
                        .ifPresent(orig -> cs.put("originalTeacher", mainApp.getTeacherName(orig.teacherId())));
                    
                    if (!cs.containsKey("originalTeacher")) cs.put("originalTeacher", "—");
                } else if (targetBase != null) {
                    cs.put("subject", mainApp.getSubjectName(targetBase.subjectId()));
                    cs.put("teacher", mainApp.getTeacherName(targetBase.teacherId()));
                    cs.put("room", mainApp.getClassroomName(targetBase.classroomId()));
                }
                
                classStatuses.add(cs);
            }
        }
        data.put("classStatuses", classStatuses);
        data.put("rows", classStatuses);

        if (currentLessonNum != -1 && !classStatuses.isEmpty()) {
            Map<String, Object> firstClass = classStatuses.get(0);
            DaySchedule.LessonInfo li = activeDs.getLessons().get(currentLessonNum - 1);
            Map<String, Object> cl = new HashMap<>();
            cl.put("number", currentLessonNum);
            cl.put("subject", isBreak ? "Наступний: " + firstClass.get("subject") : firstClass.get("subject"));
            cl.put("teacher", firstClass.get("teacher"));
            cl.put("room", firstClass.get("room"));
            cl.put("className", firstClass.get("className"));
            cl.put("start", li.start.toString());
            cl.put("end", li.end.toString());
            data.put("currentLesson", cl);
        }

        return data;
    }

    private String calculateCountdown(LocalTime now) {
        com.schoolbell.model.BellEntry nextEntry = mainApp.getSchedule().stream()
                .filter(entry -> entry.time().isAfter(now))
                .findFirst().orElse(null);
        if (nextEntry != null) {
            java.time.Duration d = java.time.Duration.between(now, nextEntry.time());
            return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
        }
        return "--:--:--";
    }
}
