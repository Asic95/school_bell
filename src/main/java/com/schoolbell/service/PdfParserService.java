package com.schoolbell.service;

import technology.tabula.*;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfParserService {
    private static final Logger logger = LoggerFactory.getLogger(PdfParserService.class);

    public String extractFullText(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            return stripper.getText(document);
        } catch (IOException e) {
            logger.error("Error extracting text from PDF: " + pdfFile.getName(), e);
            return "";
        }
    }

    public List<List<String>> extractTable(File pdfFile) {
        logger.info("=== Starting PDF Extraction: {} ===", pdfFile.getName());
        List<List<String>> bestRows = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            ObjectExtractor oe = new ObjectExtractor(document);
            SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
            BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
            
            PageIterator it = oe.extract();
            int pageNum = 0;
            while (it.hasNext()) {
                pageNum++;
                Page page = it.next();
                logger.info("Processing Page {}", pageNum);
                
                List<Table> seaTables = sea.extract(page);
                List<Table> beaTables = bea.extract(page);
                
                logger.info("  Spreadsheet Algorithm (SEA) found {} tables", seaTables.size());
                logger.info("  Basic Algorithm (BEA) found {} tables", beaTables.size());

                int seaCols = getAverageColumnCount(seaTables);
                int beaCols = getAverageColumnCount(beaTables);
                logger.info("  SEA Average Columns: {}, BEA Average Columns: {}", seaCols, beaCols);

                List<Table> tablesToUse = seaTables;
                if (beaCols > seaCols && beaCols >= 2) {
                    logger.info("  Decided to use Basic Algorithm (BEA)");
                    tablesToUse = beaTables;
                } else if ((tablesToUse == null || tablesToUse.isEmpty()) && !beaTables.isEmpty()) {
                    logger.info("  SEA empty, falling back to BEA");
                    tablesToUse = beaTables;
                } else {
                    logger.info("  Using Spreadsheet Algorithm (SEA)");
                }

                if (tablesToUse != null) {
                    for (Table table : tablesToUse) {
                        for (List<RectangularTextContainer> row : table.getRows()) {
                            List<String> cells = new ArrayList<>();
                            for (RectangularTextContainer cell : row) {
                                String text = cell.getText().trim();
                                cells.add(text);
                            }
                            if (cells.stream().anyMatch(c -> !c.isEmpty())) {
                                bestRows.add(cells);
                                logger.debug("    Extracted Row: {}", cells);
                            }
                        }
                    }
                }
            }
            logger.info("Total rows extracted from PDF: {}", bestRows.size());
            if (!bestRows.isEmpty()) {
                logger.info("First row sample: {}", bestRows.get(0));
            }
        } catch (IOException e) {
            logger.error("Error parsing PDF file: " + pdfFile.getName(), e);
        }
        return bestRows;
    }

    private int getAverageColumnCount(List<Table> tables) {
        if (tables == null || tables.isEmpty()) return 0;
        int sum = 0;
        int count = 0;
        for (Table t : tables) {
            List<List<RectangularTextContainer>> rows = t.getRows();
            if (rows != null && !rows.isEmpty()) {
                sum += rows.get(0).size();
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }
}
