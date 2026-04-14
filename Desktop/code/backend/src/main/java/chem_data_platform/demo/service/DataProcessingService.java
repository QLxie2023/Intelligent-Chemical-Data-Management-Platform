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
 * 数据处理服务
 * 整合 JSON 清洗和 Excel 生成
 * 将 Kimi API 返回的 JSON 转换为 Excel 文件供下载
 */
@Service
public class DataProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DataProcessingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理 Kimi 返回的 JSON 并生成 Excel
     * @param kimiResponse Kimi API 返回的原始 JSON 字符串
     * @param outputPath Excel 输出文件路径
     * @return Excel 文件路径
     */
    public String processAndGenerateExcel(String kimiResponse, String outputPath) {
        try {
            log.info("开始处理 JSON 并生成 Excel");
            
            // 步骤1: 清洗和解析 JSON
            Map<String, Object> parsedData = parseKimiResponse(kimiResponse);
            
            // 步骤2: 生成 Excel
            String excelPath = generateExcelFromData(parsedData, outputPath);
            log.info("Excel 生成成功: {}", excelPath);
            return excelPath;

        } catch (Exception e) {
            log.error("数据处理失败，使用兜底数据: {}", e.getMessage(), e);
            // 兜底方案：返回 Mock 数据生成的 Excel
            return generateMockExcel(outputPath);
        }
    }

    /**
     * 解析 Kimi API 响应
     * @param kimiResponse 原始 JSON 响应
     * @return 解析后的数据 Map
     */
    private Map<String, Object> parseKimiResponse(String kimiResponse) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try {
            JsonNode root = objectMapper.readTree(kimiResponse);
            
            // 提取摘要
            String summary = root.has("summary") ? root.get("summary").asText() : "暂无摘要";
            result.put("summary", summary);
            
            // 提取化合物数据
            List<Map<String, String>> compounds = new ArrayList<>();
            if (root.has("compounds") && root.get("compounds").isArray()) {
                root.get("compounds").forEach(node -> {
                    Map<String, String> compound = new HashMap<>();
                    compound.put("名称", node.has("name") ? node.get("name").asText() : "");
                    compound.put("分子式", node.has("formula") ? node.get("formula").asText() : "");
                    compound.put("CAS号", node.has("cas") ? node.get("cas").asText() : "");
                    compounds.add(compound);
                });
            }
            result.put("compounds", compounds);
            
            // 提取实验条件
            Map<String, String> conditions = new HashMap<>();
            if (root.has("conditions")) {
                JsonNode condNode = root.get("conditions");
                conditions.put("温度", condNode.has("temperature") ? condNode.get("temperature").asText() : "N/A");
                conditions.put("压力", condNode.has("pressure") ? condNode.get("pressure").asText() : "N/A");
                conditions.put("溶剂", condNode.has("solvent") ? condNode.get("solvent").asText() : "N/A");
            }
            result.put("conditions", conditions);
            
            // 提取收率数据
            List<Map<String, String>> yields = new ArrayList<>();
            if (root.has("yields") && root.get("yields").isArray()) {
                root.get("yields").forEach(node -> {
                    Map<String, String> yieldData = new HashMap<>();
                    yieldData.put("产物", node.has("product") ? node.get("product").asText() : "");
                    yieldData.put("收率(%)", node.has("yield_percent") ? node.get("yield_percent").asText() : "");
                    yieldData.put("单位", node.has("unit") ? node.get("unit").asText() : "");
                    yields.add(yieldData);
                });
            }
            result.put("yields", yields);
            
            // 提取其他数据
            if (root.has("other_data")) {
                result.put("other_data", objectMapper.convertValue(root.get("other_data"), Map.class));
            }
            
            log.info("JSON 解析成功，提取到化合物数: {}, 收率数: {}", compounds.size(), yields.size());
            return result;
            
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", e.getMessage());
            // 如果 JSON 格式不正确，返回原始数据
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("summary", "JSON 解析失败，原始数据如下");
            fallback.put("raw_data", kimiResponse);
            return fallback;
        }
    }

    /**
     * 从解析的数据生成 Excel 文件
     * @param data 解析后的数据
     * @param outputPath 输出文件路径
     * @return Excel 文件路径
     */
    private String generateExcelFromData(Map<String, Object> data, String outputPath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        try {
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // 工作表1: 摘要和基本信息
            Sheet summarySheet = workbook.createSheet("摘要");
            createSummarySheet(summarySheet, data, headerStyle, dataStyle);
            
            // 工作表2: 化合物数据
            if (data.containsKey("compounds")) {
                Sheet compoundsSheet = workbook.createSheet("化合物");
                createCompoundsSheet(compoundsSheet, (List<Map<String, String>>) data.get("compounds"), headerStyle, dataStyle);
            }
            
            // 工作表3: 实验条件
            if (data.containsKey("conditions")) {
                Sheet conditionsSheet = workbook.createSheet("实验条件");
                createConditionsSheet(conditionsSheet, (Map<String, String>) data.get("conditions"), headerStyle, dataStyle);
            }
            
            // 工作表4: 收率数据
            if (data.containsKey("yields")) {
                Sheet yieldsSheet = workbook.createSheet("收率数据");
                createYieldsSheet(yieldsSheet, (List<Map<String, String>>) data.get("yields"), headerStyle, dataStyle);
            }
            
            // 写入文件
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            
            log.info("Excel 文件已生成: {}", outputPath);
            return outputPath;
            
        } finally {
            workbook.close();
        }
    }

    /**
     * 创建摘要工作表
     */
    private void createSummarySheet(Sheet sheet, Map<String, Object> data, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // 标题
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("文献分析摘要");
        titleCell.setCellStyle(headerStyle);
        titleRow.setHeightInPoints(25);
        
        // 摘要内容
        rowNum++;
        Row summaryLabelRow = sheet.createRow(rowNum++);
        summaryLabelRow.createCell(0).setCellValue("摘要内容");
        summaryLabelRow.getCell(0).setCellStyle(headerStyle);
        
        Row summaryRow = sheet.createRow(rowNum++);
        Cell summaryCell = summaryRow.createCell(0);
        summaryCell.setCellValue(data.getOrDefault("summary", "暂无摘要").toString());
        summaryCell.setCellStyle(dataStyle);
        sheet.setColumnWidth(0, 6000);
    }

    /**
     * 创建化合物数据工作表
     */
    private void createCompoundsSheet(Sheet sheet, List<Map<String, String>> compounds, CellStyle headerStyle, CellStyle dataStyle) {
        if (compounds == null || compounds.isEmpty()) {
            sheet.createRow(0).createCell(0).setCellValue("暂无化合物数据");
            return;
        }
        
        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"名称", "分子式", "CAS号"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 3000);
        }
        
        // 数据行
        int rowNum = 1;
        for (Map<String, String> compound : compounds) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(compound.getOrDefault("名称", ""));
            row.createCell(1).setCellValue(compound.getOrDefault("分子式", ""));
            row.createCell(2).setCellValue(compound.getOrDefault("CAS号", ""));
            for (int i = 0; i < headers.length; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }
    }

    /**
     * 创建实验条件工作表
     */
    private void createConditionsSheet(Sheet sheet, Map<String, String> conditions, CellStyle headerStyle, CellStyle dataStyle) {
        // 表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("条件类型");
        headerRow.createCell(1).setCellValue("数值");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);
        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 4000);
        
        // 数据行
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
     * 创建收率数据工作表
     */
    private void createYieldsSheet(Sheet sheet, List<Map<String, String>> yields, CellStyle headerStyle, CellStyle dataStyle) {
        if (yields == null || yields.isEmpty()) {
            sheet.createRow(0).createCell(0).setCellValue("暂无收率数据");
            return;
        }
        
        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"产物", "收率(%)", "单位"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 3000);
        }
        
        // 数据行
        int rowNum = 1;
        for (Map<String, String> yield : yields) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(yield.getOrDefault("产物", ""));
            row.createCell(1).setCellValue(yield.getOrDefault("收率(%)", ""));
            row.createCell(2).setCellValue(yield.getOrDefault("单位", ""));
            for (int i = 0; i < headers.length; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }
    }

    /**
     * 创建表头样式
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
     * 创建数据样式
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
     * 获取分析摘要
     * @param kimiResponse Kimi API 返回的原始 JSON 字符串
     * @return 文献摘要文本
     */
    public String getSummary(String kimiResponse) {
        try {
            Map<String, Object> data = parseKimiResponse(kimiResponse);
            return data.getOrDefault("summary", "摘要获取失败").toString();
        } catch (Exception e) {
            log.error("摘要获取失败: {}", e.getMessage());
            return "摘要获取失败";
        }
    }

    /**
     * 兜底方案：生成 Mock 数据的 Excel
     */
    private String generateMockExcel(String outputPath) {
        try {
            Map<String, Object> mockData = new HashMap<>();
            mockData.put("summary", "这是兜底数据示例。由于原始数据解析失败，系统使用示例数据生成此 Excel 文件。");
            
            List<Map<String, String>> mockCompounds = new ArrayList<>();
            Map<String, String> compound = new HashMap<>();
            compound.put("名称", "示例化合物");
            compound.put("分子式", "C6H12O6");
            compound.put("CAS号", "50-99-7");
            mockCompounds.add(compound);
            mockData.put("compounds", mockCompounds);
            
            Map<String, String> mockConditions = new HashMap<>();
            mockConditions.put("温度", "25°C");
            mockConditions.put("压力", "1 atm");
            mockConditions.put("溶剂", "水");
            mockData.put("conditions", mockConditions);
            
            List<Map<String, String>> mockYields = new ArrayList<>();
            Map<String, String> yieldData = new HashMap<>();
            yieldData.put("产物", "产物A");
            yieldData.put("收率(%)", "85");
            yieldData.put("单位", "%");
            mockYields.add(yieldData);
            mockData.put("yields", mockYields);
            
            Workbook workbook = new XSSFWorkbook();
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // 创建摘要表
            Sheet sheet = workbook.createSheet("示例数据");
            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("【兜底数据】Kimi 分析失败，请检查日志");
            cell.setCellStyle(headerStyle);
            
            // 写入文件
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            workbook.close();
            
            log.info("兜底 Excel 已生成: {}", outputPath);
            return outputPath;
            
        } catch (Exception e) {
            log.error("兜底 Excel 生成失败: {}", e.getMessage());
            return null;
        }
    }
}
