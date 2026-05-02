package com.schoolbell.service;

import com.schoolbell.model.BellEntry;
import com.schoolbell.model.DaySchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);
    private static final int DEFAULT_DURATION = 5;
    private static final String INTERNAL_SCHEDULE_FILE = "schedules.properties";

    public List<DaySchedule> loadInternalSchedules() {
        List<DaySchedule> schedules = new ArrayList<>();
        Properties props = new Properties();
        File file = new File(INTERNAL_SCHEDULE_FILE);
        if (!file.exists()) {
            DaySchedule defaultSch = new DaySchedule("Стандартний");
            schedules.add(defaultSch);
            saveInternalSchedules(schedules);
            return schedules;
        }

        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            String namesStr = props.getProperty("schedules.list", "");
            if (namesStr.isEmpty()) return schedules;

            String[] names = namesStr.split(",");
            for (String name : names) {
                DaySchedule ds = new DaySchedule(name);
                List<DaySchedule.LessonInfo> lessons = new ArrayList<>();
                for (int i = 1; i <= 7; i++) {
                    String start = props.getProperty("sch." + name + "." + i + ".start");
                    String end = props.getProperty("sch." + name + "." + i + ".end");
                    String brk = props.getProperty("sch." + name + "." + i + ".break", "0");
                    
                    DaySchedule.LessonInfo li = new DaySchedule.LessonInfo();
                    if (start != null && !start.isEmpty()) li.start = LocalTime.parse(start);
                    if (end != null && !end.isEmpty()) li.end = LocalTime.parse(end);
                    li.breakAfterMinutes = Integer.parseInt(brk);
                    lessons.add(li);
                }
                ds.setLessons(lessons);
                schedules.add(ds);
            }
        } catch (Exception e) {
            logger.error("Failed to load internal schedules", e);
        }
        return schedules;
    }

    public void saveInternalSchedules(List<DaySchedule> schedules) {
        Properties props = new Properties();
        StringBuilder names = new StringBuilder();
        for (DaySchedule ds : schedules) {
            if (names.length() > 0) names.append(",");
            names.append(ds.getName());
            
            List<DaySchedule.LessonInfo> lessons = ds.getLessons();
            for (int i = 0; i < lessons.size(); i++) {
                DaySchedule.LessonInfo li = lessons.get(i);
                int idx = i + 1;
                if (li.start != null) props.setProperty("sch." + ds.getName() + "." + idx + ".start", li.start.toString());
                if (li.end != null) props.setProperty("sch." + ds.getName() + "." + idx + ".end", li.end.toString());
                props.setProperty("sch." + ds.getName() + "." + idx + ".break", String.valueOf(li.breakAfterMinutes));
            }
        }
        props.setProperty("schedules.list", names.toString());
        
        try (FileOutputStream out = new FileOutputStream(INTERNAL_SCHEDULE_FILE)) {
            props.store(out, "Internal School Bell Schedules");
        } catch (IOException e) {
            logger.error("Failed to save internal schedules", e);
        }
    }

    public List<BellEntry> convertToBellEntries(DaySchedule daySchedule) {
        List<BellEntry> entries = new ArrayList<>();
        int lessonNum = 1;
        for (DaySchedule.LessonInfo li : daySchedule.getLessons()) {
            if (li.start != null) {
                entries.add(new BellEntry(li.start, DEFAULT_DURATION, lessonNum + " урок (початок)"));
            }
            if (li.end != null) {
                entries.add(new BellEntry(li.end, DEFAULT_DURATION, lessonNum + " урок (кінець)"));
            }
            lessonNum++;
        }
        entries.sort((a, b) -> a.time().compareTo(b.time()));
        return entries;
    }
}
