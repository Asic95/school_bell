package com.schoolbell.service;

import com.schoolbell.model.BellEntry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    private static final int DEFAULT_DURATION = 5;

    public List<String> getSheetNames(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                names.add(workbook.getSheetName(i));
            }
            return names;
        }
    }

    public List<BellEntry> loadSchedule(String filePath, String sheetName) throws IOException {
        List<BellEntry> schedule = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    if (workbook.getSheetName(i).equalsIgnoreCase(sheetName)) {
                        sheet = workbook.getSheetAt(i);
                        break;
                    }
                }
            }
            if (sheet == null) return schedule;

            for (Row row : sheet) {
                // Skip header rows (Rows 0 and 1 in Excel are 0 and 1 in POI)
                if (row.getRowNum() < 2) continue;

                try {
                    // Col 1: Lesson label
                    Cell cell1 = row.getCell(1);
                    String label = "Урок";
                    if (cell1 != null) {
                        if (cell1.getCellType() == CellType.NUMERIC) {
                            label = (int)cell1.getNumericCellValue() + " урок";
                        } else if (cell1.getCellType() == CellType.STRING) {
                            label = cell1.getStringCellValue();
                        }
                    }
                    
                    if (label.toLowerCase().contains("урок")) {
                        // If it still says "Урок", it's likely a header row we missed
                        if (row.getCell(2) != null && row.getCell(2).getCellType() == CellType.STRING) {
                            if (row.getCell(2).getStringCellValue().toLowerCase().contains("початок")) continue;
                        }
                    }

                    // Col 2: Start time
                    LocalTime tStart = parseTime(row.getCell(2));
                    if (tStart != null) {
                        schedule.add(new BellEntry(tStart, DEFAULT_DURATION, label + " (початок)"));
                    }
                    
                    // Col 3: End time
                    LocalTime tEnd = parseTime(row.getCell(3));
                    if (tEnd != null) {
                        schedule.add(new BellEntry(tEnd, DEFAULT_DURATION, label + " (кінець)"));
                    }
                } catch (Exception e) {
                    // Skip invalid rows
                }
            }
        }
        
        schedule.sort((a, b) -> a.time().compareTo(b.time()));
        logger.info("Loaded {} bell entries from sheet '{}'", schedule.size(), sheetName);
        return schedule;
    }

    private LocalTime parseTime(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalTime();
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                if (val.isEmpty()) return null;
                return LocalTime.parse(val, TIME_FORMATTER);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
