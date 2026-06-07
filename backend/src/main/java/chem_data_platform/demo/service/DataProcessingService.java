package chem_data_platform.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import chem_data_platform.demo.entity.AnalysisResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * Data processing service
 * Integrates JSON cleanup and Excel generation
 * Convert JSON analysis results into an Excel file for download
 */
@Service
public class DataProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DataProcessingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process JSON analysis results and generate Excel
     * @param aiResponse Raw JSON analysis result
     * @param outputPath Excel output file path
     * @return Excel file path
     */
    public String processAndGenerateExcel(String aiResponse, String outputPath) {
        try {
            log.info("Start processing JSON and generating Excel");
            
            // Step 1: clean and parse JSON
            Map<String, Object> parsedData = parseAnalysisResponse(aiResponse);
            
            // Step 2: generate Excel
            String excelPath = generateExcelFromData(parsedData, outputPath);
            log.info("Excel generated successfully: {}", excelPath);
            return excelPath;

        } catch (Exception e) {
            log.error("Data processing failed; using fallback data: {}", e.getMessage(), e);
            // Fallback strategy: generate Excel from mock data
            return generateMockExcel(outputPath);
        }
    }

    /**
     * Parse AI analysis response
     * @param aiResponse Raw JSON response
     * @return Parsed data map
     */
    private Map<String, Object> parseAnalysisResponse(String aiResponse) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            
            // Extract summary
            String summary = root.has("summary") ? root.get("summary").asText() : "No summary available";
            result.put("summary", summary);
            
            // Extract compound data
            List<Map<String, String>> compounds = new ArrayList<>();
            if (root.has("compounds") && root.get("compounds").isArray()) {
                root.get("compounds").forEach(node -> {
                    Map<String, String> compound = new HashMap<>();
                    compound.put("Name", node.has("name") ? node.get("name").asText() : "");
                    compound.put("Molecular Formula", node.has("formula") ? node.get("formula").asText() : "");
                    compound.put("CAS Number", node.has("cas") ? node.get("cas").asText() : "");
                    compounds.add(compound);
                });
            }
            result.put("compounds", compounds);
            
            // Extract experimental conditions
            Map<String, String> conditions = new HashMap<>();
            if (root.has("conditions")) {
                JsonNode condNode = root.get("conditions");
                conditions.put("Temperature", condNode.has("temperature") ? condNode.get("temperature").asText() : "N/A");
                conditions.put("Pressure", condNode.has("pressure") ? condNode.get("pressure").asText() : "N/A");
                conditions.put("Solvent", condNode.has("solvent") ? condNode.get("solvent").asText() : "N/A");
            }
            result.put("conditions", conditions);
            
            // Extract yield data
            List<Map<String, String>> yields = new ArrayList<>();
            if (root.has("yields") && root.get("yields").isArray()) {
                root.get("yields").forEach(node -> {
                    Map<String, String> yieldData = new HashMap<>();
                    yieldData.put("Product", node.has("product") ? node.get("product").asText() : "");
                    yieldData.put("Yield (%)", node.has("yield_percent") ? node.get("yield_percent").asText() : "");
                    yieldData.put("Unit", node.has("unit") ? node.get("unit").asText() : "");
                    yields.add(yieldData);
                });
            }
            result.put("yields", yields);
            
            // Extract other data
            if (root.has("other_data")) {
                result.put("other_data", objectMapper.convertValue(root.get("other_data"), Map.class));
            }
            
            log.info("JSON parsed successfully; compound count: {}, yield count: {}", compounds.size(), yields.size());
            return result;
            
        } catch (Exception e) {
            log.error("JSON parsing failed: {}", e.getMessage());
            // If the JSON format is invalid, return the raw data
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("summary", "JSON parsing failed. Raw data follows");
            fallback.put("raw_data", aiResponse);
            return fallback;
        }
    }

    /**
     * Generate an Excel file from parsed data
     * @param data Parsed data
     * @param outputPath Output file path
     * @return Excel file path
     */
    private String generateExcelFromData(Map<String, Object> data, String outputPath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        try {
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // Worksheet 1: summary and basic information
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, data, headerStyle, dataStyle);
            
            // Worksheet 2: compound data
            if (data.containsKey("compounds")) {
                Sheet compoundsSheet = workbook.createSheet("Compounds");
                createCompoundsSheet(compoundsSheet, (List<Map<String, String>>) data.get("compounds"), headerStyle, dataStyle);
            }
            
            // Worksheet 3: experimental conditions
            if (data.containsKey("conditions")) {
                Sheet conditionsSheet = workbook.createSheet("Experimental Conditions");
                createConditionsSheet(conditionsSheet, (Map<String, String>) data.get("conditions"), headerStyle, dataStyle);
            }
            
            // Worksheet 4: yield data
            if (data.containsKey("yields")) {
                Sheet yieldsSheet = workbook.createSheet("Yield Data");
                createYieldsSheet(yieldsSheet, (List<Map<String, String>>) data.get("yields"), headerStyle, dataStyle);
            }
            
            // Write file
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            
            log.info("Excel file generated: {}", outputPath);
            return outputPath;
            
        } finally {
            workbook.close();
        }
    }

    /**
     * Create summary worksheet
     */
    private void createSummarySheet(Sheet sheet, Map<String, Object> data, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Literature Analysis Summary");
        titleCell.setCellStyle(headerStyle);
        titleRow.setHeightInPoints(25);
        
        // Summary content
        rowNum++;
        Row summaryLabelRow = sheet.createRow(rowNum++);
        summaryLabelRow.createCell(0).setCellValue("Summary content");
        summaryLabelRow.getCell(0).setCellStyle(headerStyle);
        
        Row summaryRow = sheet.createRow(rowNum++);
        Cell summaryCell = summaryRow.createCell(0);
        summaryCell.setCellValue(data.getOrDefault("summary", "No summary available").toString());
        summaryCell.setCellStyle(dataStyle);
        sheet.setColumnWidth(0, 6000);
    }

    /**
     * Create compound data worksheet
     */
    private void createCompoundsSheet(Sheet sheet, List<Map<String, String>> compounds, CellStyle headerStyle, CellStyle dataStyle) {
        if (compounds == null || compounds.isEmpty()) {
            sheet.createRow(0).createCell(0).setCellValue("No compound data available");
            return;
        }
        
        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Name", "Molecular Formula", "CAS Number"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 3000);
        }
        
        // Data row
        int rowNum = 1;
        for (Map<String, String> compound : compounds) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(compound.getOrDefault("Name", ""));
            row.createCell(1).setCellValue(compound.getOrDefault("Molecular Formula", ""));
            row.createCell(2).setCellValue(compound.getOrDefault("CAS Number", ""));
            for (int i = 0; i < headers.length; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }
    }

    /**
     * Create experimental conditions worksheet
     */
    private void createConditionsSheet(Sheet sheet, Map<String, String> conditions, CellStyle headerStyle, CellStyle dataStyle) {
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Condition Type");
        headerRow.createCell(1).setCellValue("Value");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);
        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 4000);
        
        // Data row
        int rowNum = 1;
        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
            row.getCell(0).setCellStyle(dataStyle);
            row.getCell(1).setCellStyle(dataStyle);
        }
    }

    /**
     * Create yield data worksheet
     */
    private void createYieldsSheet(Sheet sheet, List<Map<String, String>> yields, CellStyle headerStyle, CellStyle dataStyle) {
        if (yields == null || yields.isEmpty()) {
            sheet.createRow(0).createCell(0).setCellValue("No yield data available");
            return;
        }
        
        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Product", "Yield (%)", "Unit"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 3000);
        }
        
        // Data row
        int rowNum = 1;
        for (Map<String, String> yield : yields) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(yield.getOrDefault("Product", ""));
            row.createCell(1).setCellValue(yield.getOrDefault("Yield (%)", ""));
            row.createCell(2).setCellValue(yield.getOrDefault("Unit", ""));
            for (int i = 0; i < headers.length; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }
    }

    /**
     * Create header row style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Create data style
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    /**
     * Get analysis summary
     * @param aiResponse Raw JSON analysis result
     * @return Literature summary text
     */
    public String getSummary(String aiResponse) {
        try {
            Map<String, Object> data = parseAnalysisResponse(aiResponse);
            return data.getOrDefault("summary", "Summary retrieval failed").toString();
        } catch (Exception e) {
            log.error("Summary retrieval failed: {}", e.getMessage());
            return "Summary retrieval failed";
        }
    }

    /**
     * Fallback strategy: generate Excel from mock data
     */
    private String generateMockExcel(String outputPath) {
        try {
            Map<String, Object> mockData = new HashMap<>();
            mockData.put("summary", "This is fallback sample data. Because the original data could not be parsed, the system generated this Excel file from sample data.");
            
            List<Map<String, String>> mockCompounds = new ArrayList<>();
            Map<String, String> compound = new HashMap<>();
            compound.put("Name", "Sample Compound");
            compound.put("Molecular Formula", "C6H12O6");
            compound.put("CAS Number", "50-99-7");
            mockCompounds.add(compound);
            mockData.put("compounds", mockCompounds);
            
            Map<String, String> mockConditions = new HashMap<>();
            mockConditions.put("Temperature", "25°C");
            mockConditions.put("Pressure", "1 atm");
            mockConditions.put("Solvent", "Water");
            mockData.put("conditions", mockConditions);
            
            List<Map<String, String>> mockYields = new ArrayList<>();
            Map<String, String> yieldData = new HashMap<>();
            yieldData.put("Product", "ProductA");
            yieldData.put("Yield (%)", "85");
            yieldData.put("Unit", "%");
            mockYields.add(yieldData);
            mockData.put("yields", mockYields);
            
            Workbook workbook = new XSSFWorkbook();
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // Create summary sheet
            Sheet sheet = workbook.createSheet("Sample Data");
            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("[Fallback Data] AI analysis failed. Please check the logs");
            cell.setCellStyle(headerStyle);
            
            // Write file
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            workbook.close();
            
            log.info("Fallback Excel generated: {}", outputPath);
            return outputPath;
            
        } catch (Exception e) {
            log.error("Fallback Excel generation failed: {}", e.getMessage());
            return null;
        }
    }
}
