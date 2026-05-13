package com.schoolbell.service;

import com.schoolbell.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleDataNormalizer {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleDataNormalizer.class);
    private final AcademicService academicService;

    // Cache to minimize DB hits
    private final Map<String, Integer> teacherCache = new HashMap<>();
    private final Map<String, Integer> subjectCache = new HashMap<>();
    private final Map<String, Integer> roomCache = new HashMap<>();
    private final Map<String, Integer> classCache = new HashMap<>();

    public ScheduleDataNormalizer(AcademicService academicService) {
        this.academicService = academicService;
        refreshCaches();
    }

    public void refreshCaches() {
        teacherCache.clear();
        subjectCache.clear();
        roomCache.clear();
        classCache.clear();
        academicService.getAllTeachers().forEach(t -> teacherCache.put(t.name(), t.id()));
        academicService.getAllSubjects().forEach(s -> subjectCache.put(s.name(), s.id()));
        academicService.getAllClassrooms().forEach(r -> roomCache.put(r.name(), r.id()));
        academicService.getAllClasses().forEach(c -> classCache.put(c.name(), c.id()));
    }

    public record ImportReport(
            List<ParsedEntry> entries,
            Set<String> newTeachers,
            Set<String> newSubjects,
            Set<String> newClasses,
            Set<String> newRooms,
            int totalLessons
    ) {
        public boolean isEmpty() { return entries.isEmpty(); }
    }

    public record ParsedEntry(int dayOfWeek, int lessonIndex, ParsedCell cell) {}

    public ImportReport analyzeImport(List<List<String>> rows, String fullText) {
        logger.info("=== Starting Normalizer Analysis: {} rows ===", rows.size());
        List<ParsedEntry> entries = new ArrayList<>();
        Set<String> newTeachers = new HashSet<>();
        Set<String> newSubjects = new HashSet<>();
        Set<String> newClasses = new HashSet<>();
        Set<String> newRooms = new HashSet<>();

        if (rows.isEmpty()) return new ImportReport(entries, newTeachers, newSubjects, newClasses, newRooms, 0);

        // Detect Global Default Class from header or full text
        String defaultClass = "";
        Pattern classHeaderPattern = Pattern.compile("(?U)для\\s+(\\d+[-а-яА-ЯіїєґІЇЄҐ]*)\\s+класу");
        
        // Try full text first (usually more reliable for titles)
        if (fullText != null && !fullText.isEmpty()) {
            Matcher m = classHeaderPattern.matcher(fullText);
            if (m.find()) {
                defaultClass = m.group(1).trim();
                logger.info("Detected Global Default Class from full text: {}", defaultClass);
            }
        }
        
        // Fallback to row-based detection
        if (defaultClass.isEmpty()) {
            for (int i = 0; i < Math.min(5, rows.size()); i++) {
                for (String cell : rows.get(i)) {
                    Matcher m = classHeaderPattern.matcher(cell);
                    if (m.find()) {
                        defaultClass = m.group(1).trim();
                        logger.info("Detected Global Default Class from row {}: {}", i, defaultClass);
                        break;
                    }
                }
                if (!defaultClass.isEmpty()) break;
            }
        }

        if (!defaultClass.isEmpty() && !defaultClass.toLowerCase().contains("кл")) {
            defaultClass += " клас";
        }

        int startRow = -1;
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.isEmpty()) continue;
            String firstCell = row.get(0).trim();
            if (firstCell.matches("(?i)^1\\.?.*") || firstCell.equalsIgnoreCase("Урок")) {
                startRow = i;
                if (firstCell.equalsIgnoreCase("Урок")) startRow++;
                logger.info("Found table start at row {}", startRow);
                break;
            }
        }
        
        if (startRow == -1) startRow = 0;

        for (int rowIndex = startRow; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (row.size() < 2) continue;

            String firstCell = row.get(0).trim();
            Pattern lessonPattern = Pattern.compile("^(\\d+)\\.?");
            Matcher lessonMatcher = lessonPattern.matcher(firstCell);
            
            int lessonIndex;
            if (lessonMatcher.find()) {
                lessonIndex = Integer.parseInt(lessonMatcher.group(1));
                logger.info("Processing Lesson row {}", lessonIndex);
            } else {
                continue;
            }

            for (int dayOfWeek = 1; dayOfWeek < row.size(); dayOfWeek++) {
                if (dayOfWeek > 7) break; 

                String cellContent = row.get(dayOfWeek).trim();
                if (cellContent.isEmpty() || cellContent.equalsIgnoreCase("null")) continue;

                ParsedCell parsed = parseCell(cellContent);
                if (parsed != null) {
                    // ALWAYS use global default class as requested by user
                    parsed = new ParsedCell(parsed.subject(), parsed.teacher(), parsed.room(), defaultClass);

                    logger.info("  Day {}: Detected [{} | {} | {} | {}]", dayOfWeek, parsed.subject(), parsed.teacher(), parsed.room(), parsed.schoolClass());
                    entries.add(new ParsedEntry(dayOfWeek, lessonIndex, parsed));
                    
                    if (!teacherCache.containsKey(parsed.teacher())) newTeachers.add(parsed.teacher());
                    if (!subjectCache.containsKey(parsed.subject())) newSubjects.add(parsed.subject());
                    if (!parsed.schoolClass().isEmpty() && !classCache.containsKey(parsed.schoolClass())) newClasses.add(parsed.schoolClass());
                    if (!parsed.room().isEmpty() && !roomCache.containsKey(parsed.room())) newRooms.add(parsed.room());
                } else {
                    logger.warn("  Day {}: FAILED to parse cell: [{}]", dayOfWeek, cellContent.replace("\n", " | "));
                }
            }
        }

        logger.info("Analysis Complete: {} entries found", entries.size());
        return new ImportReport(entries, newTeachers, newSubjects, newClasses, newRooms, entries.size());
    }

    public void executeImport(ImportReport report) {
        logger.info("=== Executing Import: {} entries ===", report.entries().size());
        for (ParsedEntry entry : report.entries()) {
            saveNormalizedEntry(entry.dayOfWeek(), entry.lessonIndex(), entry.cell());
        }
        refreshCaches();
    }

    public ParsedCell parseCell(String content) {
        if (content == null || content.isBlank()) return null;

        // Standardize text
        content = content.replace("ʼ", "'").replace("\r", "");
        
        // Targeted fix for known PDF artifacts from NZ.UA/Tabula
        content = content.replace("тадобробут", "та добробут");
        content = content.replace("ісуспільство", "і суспільство");
        
        // Inject spaces before metadata keywords if missing
        String keywords = "Приміщення|кабінет|зала|зал|класу|Група|ГПД|Інформатичний|Спортивна|Спортивний|Музична|Музичний";
        content = content.replaceAll("(?U)([^\\s\\[])(" + keywords + ")", "$1 $2");

        // 1. Separate Metadata (Room info in brackets) from the main text
        // As per user request: Room is ONLY from brackets [...]
        Pattern bracketPattern = Pattern.compile("\\[(.*?)\\]");
        Matcher bracketMatcher = bracketPattern.matcher(content);
        
        String room = "";
        String mainText = content;
        
        // Find ALL brackets, use the last one for room as it's usually "at the end"
        int lastBracketStart = -1;
        while (bracketMatcher.find()) {
            room = bracketMatcher.group(1).trim();
            lastBracketStart = bracketMatcher.start();
        }
        
        if (lastBracketStart != -1) {
            mainText = content.substring(0, lastBracketStart).trim();
        }

        // 2. Smart Split Subject and Teacher from mainText
        String subject = "";
        String teacher = "";

        String[] lines = mainText.split("\\n");
        if (lines.length >= 2) {
            subject = lines[0].trim();
            StringBuilder tb = new StringBuilder();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                if (!tb.isEmpty()) tb.append(" ");
                tb.append(line);
            }
            teacher = tb.toString().trim();
        } else {
            String[] split = splitByCamelCase(mainText);
            if (split.length >= 2) {
                subject = split[0];
                StringBuilder tb = new StringBuilder();
                for (int i = 1; i < split.length; i++) {
                    if (!tb.isEmpty()) tb.append(" ");
                    tb.append(split[i]);
                }
                teacher = tb.toString().trim();
            } else {
                subject = mainText;
                teacher = "Невідомий вчитель";
            }
        }

        // Subject cleanup if it contains CamelCase (e.g. "MathBolshakov")
        if (subject.matches("(?U).*\\p{Ll}\\p{Lu}.*")) {
             String[] sSplit = splitByCamelCase(subject);
             if (sSplit.length >= 2) {
                 subject = sSplit[0];
                 StringBuilder tb = new StringBuilder();
                 for(int i=1; i<sSplit.length; i++) {
                     if(!tb.isEmpty()) tb.append(" ");
                     tb.append(sSplit[i]);
                 }
                 teacher = tb.toString() + (teacher.equals("Невідомий вчитель") ? "" : " " + teacher);
             }
        }

        // Final cleanup: prune ANY metadata keywords from teacher name
        Pattern prunePattern = Pattern.compile("(?U)(" + keywords + ")");
        Matcher pm = prunePattern.matcher(teacher);
        if (pm.find()) {
            teacher = teacher.substring(0, pm.start()).trim();
        }

        if (subject.isEmpty()) return null;
        // SchoolClass here will be overridden by the global class in analyzeImport
        return new ParsedCell(subject, teacher, room, "");
    }

    private String[] splitByCamelCase(String text) {
        // Split where [lowercase or dot or їіїєґ] is followed by [uppercase]
        // Using \p{Ll} for lowercase letter and \p{Lu} for uppercase letter
        return text.split("(?U)(?<=[\\p{Ll}\\u0456\\u0457\\u0454\\u0491.])(?=[\\p{Lu}\\u0406\\u0407\\u0404\\u0490])");
    }

    private ParsedMetadata extractMetadata(String text) {
        if (text == null || text.isBlank()) return new ParsedMetadata("", "");
        
        String room = "";
        String schoolClass = "";
        
        // Standardize meta text (fix missing spaces)
        text = text.replaceAll("(?U)([^\\s\\[])(Приміщення|кабінет|зала|зал|класу|Група|ГПД|Інформатичний|Спортивна|Спортивний|Музична|Музичний)", "$1 $2");

        // Handle bracketed info [5кл], [С.З.], etc.
        // As per user request: Room is primarily what's inside brackets (if it's not a class)
        Pattern bracketPattern = Pattern.compile("\\[(.*?)\\]");
        Matcher m = bracketPattern.matcher(text);
        while (m.find()) {
            String val = m.group(1).trim();
            if (val.toLowerCase().contains("кл")) {
                schoolClass = val;
            } else {
                room = val;
            }
        }

        // Fallback for room number if not found in brackets (e.g., "Приміщення 5")
        // We take only the number/ID to keep it clean as requested.
        if (room.isEmpty()) {
            Pattern p = Pattern.compile("(?U)(?:Приміщення|кабінет|зал|зала|Група)\\s+([^\\s\\[\\]]+)");
            Matcher pm = p.matcher(text);
            if (pm.find()) {
                room = pm.group(1).trim();
            }
        }

        // Handle keywords ONLY for class detection fallback
        if (text.contains("класу") && schoolClass.isEmpty()) {
            Pattern p = Pattern.compile("(?U)(\\d+[-а-яА-ЯіїєґІЇЄҐ]*)\\s+класу");
            Matcher pm = p.matcher(text);
            if (pm.find()) {
                schoolClass = pm.group(1).trim();
                if (!schoolClass.toLowerCase().contains("кл")) schoolClass += "кл";
            }
        }
        
        return new ParsedMetadata(room, schoolClass);
    }

    private record ParsedMetadata(String room, String schoolClass) {}

    private void saveNormalizedEntry(int dayOfWeek, int lessonIndex, ParsedCell parsed) {
        int subjectId = getOrCreateSubject(parsed.subject());
        int teacherId = getOrCreateTeacher(parsed.teacher());
        int roomId = getOrCreateRoom(parsed.room());
        int classId = getOrCreateClass(parsed.schoolClass());

        if (classId == 0) return;
        academicService.linkTeacherToSubject(teacherId, subjectId);
        academicService.saveScheduleEntry(classId, dayOfWeek, lessonIndex, teacherId, subjectId, roomId, 0);
    }

    private int getOrCreateTeacher(String name) {
        if (name == null || name.isBlank() || name.equals("Невідомий вчитель")) return 0;
        if (!teacherCache.containsKey(name)) {
            academicService.addTeacher(name);
            academicService.getAllTeachers().stream()
                    .filter(t -> t.name().equals(name)).findFirst()
                    .ifPresent(t -> teacherCache.put(name, t.id()));
        }
        return teacherCache.getOrDefault(name, 0);
    }

    private int getOrCreateSubject(String name) {
        if (name == null || name.isBlank()) return 0;
        if (!subjectCache.containsKey(name)) {
            academicService.addSubject(name);
            academicService.getAllSubjects().stream()
                    .filter(s -> s.name().equals(name)).findFirst()
                    .ifPresent(s -> subjectCache.put(name, s.id()));
        }
        return subjectCache.getOrDefault(name, 0);
    }

    private int getOrCreateRoom(String name) {
        if (name == null || name.isBlank()) return 0;
        if (!roomCache.containsKey(name)) {
            academicService.addClassroom(name);
            academicService.getAllClassrooms().stream()
                    .filter(r -> r.name().equals(name)).findFirst()
                    .ifPresent(r -> roomCache.put(name, r.id()));
        }
        return roomCache.getOrDefault(name, 0);
    }

    private int getOrCreateClass(String name) {
        if (name == null || name.isBlank()) return 0;
        if (!classCache.containsKey(name)) {
            academicService.addClass(name);
            academicService.getAllClasses().stream()
                    .filter(c -> c.name().equals(name)).findFirst()
                    .ifPresent(c -> classCache.put(name, c.id()));
        }
        return classCache.getOrDefault(name, 0);
    }

    public record ParsedCell(String subject, String teacher, String room, String schoolClass) {}
}
