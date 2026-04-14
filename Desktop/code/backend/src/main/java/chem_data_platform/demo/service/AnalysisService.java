package chem_data_platform.demo.service;

import chem_data_platform.demo.entity.AnalysisResult;
import chem_data_platform.demo.entity.ChemicalInfo;
import chem_data_platform.demo.repository.AnalysisResultRepository;
import chem_data_platform.demo.repository.ChemicalInfoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private final AnalysisResultRepository analysisResultRepository;
    private final ChemicalInfoRepository chemicalInfoRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeterRegistry meterRegistry;    @Value("${analysis.provider:kimi}")
    private String analysisProvider;

    @Autowired(required = false)
    private KimiService kimiService;

    public AnalysisService(AnalysisResultRepository analysisResultRepository,
                           ChemicalInfoRepository chemicalInfoRepository,
                           MeterRegistry meterRegistry) {
        this.analysisResultRepository = analysisResultRepository;
        this.chemicalInfoRepository = chemicalInfoRepository;
        this.meterRegistry = meterRegistry;
        this.webClient = WebClient.builder().build();
    }    /**
     * 根据配置选择分析提供程序（仅 Kimi AI）
     * - 使用 Kimi 上传文件并分析
     */
    public AnalysisResult analyzeFile(Long projectId, Long fileId, File file) {
        if (kimiService != null && kimiService.isConfigured()) {
            return analyzeFileWithKimi(projectId, fileId, file);
        } else {
            throw new IllegalStateException("Kimi 服务未配置。请在 application.properties 中设置 kimi.api-key");
        }
    }    /**
     * 使用 Kimi 分析文件
     * 注意：新流程中直接返回 AnalysisResult，由 DataUploadController 异步保存到 file_infos
     * 不再写入 analysis_results 表（该表已废弃）
     */
    private AnalysisResult analyzeFileWithKimi(Long projectId, Long fileId, File file) {
        log.info("Analyzing file with Kimi, projectId: {}, fileId: {}", projectId, fileId);
        System.out.println("\n========== 🔍 AnalysisService.analyzeFileWithKimi 开始 ==========");
        System.out.println("projectId: " + projectId + ", fileId: " + fileId);
        try {
            // 构建一个简单的 MultipartFile 适配器
            org.springframework.web.multipart.MultipartFile multipartFile = 
                createMultipartFileFromFile(file);

            // 第一步：上传文件到 Kimi
            System.out.println("第一步: 上传文件到 Kimi...");
            String fileId_kimi = kimiService.uploadFileToKimi(multipartFile);
            log.info("✅ 文件已上传到 Kimi, kimi_file_id: {}", fileId_kimi);
            System.out.println("✅ Kimi fileId: " + fileId_kimi);

            // 第二步：调用 Kimi 分析（使用标准化的 prompt）
            System.out.println("第二步: 调用 Kimi 分析...");
            // 构造一个标准化的 prompt，要求 Markdown 表格格式
            String customPrompt = buildAnalysisPrompt();
            String kimiRawResponse = kimiService.analyzeFileWithKimi(fileId_kimi, customPrompt);
            log.info("✅ Kimi 分析完成，结果长度: {}", kimiRawResponse != null ? kimiRawResponse.length() : 0);
            System.out.println("✅ 分析完成，响应长度: " + (kimiRawResponse != null ? kimiRawResponse.length() : 0));
            System.out.println("📄 原始响应前200字: " + (kimiRawResponse != null ? kimiRawResponse.substring(0, Math.min(200, kimiRawResponse.length())) : "null"));

            // 第三步：格式化响应数据
            // Kimi 返回的是 Chat API 响应中提取的纯文本内容或 Markdown 表格
            System.out.println("第三步: 格式化响应...");
            String formattedResponse = formatKimiResponse(kimiRawResponse);
            System.out.println("✅ 格式化完成，输出长度: " + formattedResponse.length());
            System.out.println("📄 格式化后前500字: " + formattedResponse.substring(0, Math.min(500, formattedResponse.length())));

            // 创建 AnalysisResult 对象（仅用于返回，不保存到数据库）
            AnalysisResult ar = new AnalysisResult();
            ar.setProjectId(projectId);
            ar.setFileId(fileId);
            ar.setRawResponse(formattedResponse);  // 使用格式化后的响应
            ar.setSummary(extractSummaryFromResponse(formattedResponse, 500));
            ar.setCreatedAt(LocalDateTime.now());

            meterRegistry.counter("analysis.requests", "provider", "kimi").increment();

            System.out.println("========== ✅ AnalysisService.analyzeFileWithKimi 完成 ==========\n");
            return ar;

        } catch (Exception e) {
            log.error("❌ Kimi 分析失败: {}", e.getMessage(), e);
            System.err.println("❌ AnalysisService 异常: " + e.getMessage());
            e.printStackTrace();
            AnalysisResult ar = new AnalysisResult();
            ar.setProjectId(projectId);
            ar.setFileId(fileId);
            ar.setRawResponse("kimi-error:" + e.getMessage());
            ar.setSummary("kimi-error:" + e.getMessage());
            ar.setCreatedAt(LocalDateTime.now());
            return ar;
        }
    }    /**
     * 构建分析 prompt - 要求返回 JSON 格式的结构化数据
     */
    private String buildAnalysisPrompt() {
        return "请分析上传的文档，提取所有关键实验数据。返回格式必须是以下 JSON 结构（无需 markdown 表格）：\n\n"
             + "{\n"
             + "  \"summary\": \"文档的简要摘要（100-200字）\",\n"
             + "  \"tableData\": [\n"
             + "    {\"parameter\": \"参数1\", \"value\": \"数值1\", \"unit\": \"单位1\", \"remarks\": \"备注1\"},\n"
             + "    {\"parameter\": \"参数2\", \"value\": \"数值2\", \"unit\": \"单位2\", \"remarks\": \"备注2\"}\n"
             + "  ]\n"
             + "}\n\n"
             + "说明：\n"
             + "1. summary: 文档内容的简要描述\n"
             + "2. tableData: 数组形式，每个对象代表一行数据，包含 parameter(参数名), value(数值), unit(单位), remarks(备注)\n"
             + "3. 如果文档有多个实验对比，请为每组数据创建一个表格行\n"
             + "4. 必须返回有效的 JSON 格式，不要包含 markdown 语法";
    }

    /**
     * 将 File 转换为 MultipartFile
     */
    private org.springframework.web.multipart.MultipartFile createMultipartFileFromFile(File file) throws IOException {
        return new org.springframework.web.multipart.MultipartFile() {
            @Override
            public String getName() {
                return file.getName();
            }

            @Override
            public String getOriginalFilename() {
                return file.getName();
            }

            @Override
            public String getContentType() {
                return "application/octet-stream";
            }

            @Override
            public boolean isEmpty() {
                return file.length() == 0;
            }

            @Override
            public long getSize() {
                return file.length();
            }

            @Override
            public byte[] getBytes() throws IOException {
                return Files.readAllBytes(file.toPath());
            }

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                return new java.io.FileInputStream(file);
            }

            @Override
            public org.springframework.core.io.Resource getResource() {
                return new org.springframework.core.io.FileSystemResource(file);
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                Files.copy(file.toPath(), dest.toPath());
            }

            @Override
            public void transferTo(java.nio.file.Path dest) throws IOException, IllegalStateException {
                Files.copy(file.toPath(), dest);
            }
        };
    }

        /**
     * 解析并保存化学信息
     * 注意：此方法已废弃，新流程中不再使用 AnalysisResult 表
     */
    @Deprecated
    private void parseAndSaveChemicalInfo(AnalysisResult saved, Long projectId, Long fileId, String responseBody) {
        // 此方法已被 Kimi 分析取代，不再使用
    }

    public AnalysisResult analyzeImage(Long projectId, Long imageId, File image) {
        // 复用 analyzeFile（image 作为 file 发送）
        return analyzeFile(projectId, imageId, image);
    }    /**
     * 将已经获得的原始响应保存为 AnalysisResult，并返回保存后的实体。
     * 注意：新流程中不再写入 analysis_results 表，而是写入 file_infos 表
     * 此方法已废弃，仅返回内存对象不持久化
     */
    public AnalysisResult saveRawResponse(Long projectId, Long fileId, String rawJson) {
        AnalysisResult ar = new AnalysisResult();
        ar.setProjectId(projectId);
        ar.setFileId(fileId);
        ar.setRawResponse(rawJson == null ? "" : rawJson);
        ar.setSummary(truncate(rawJson, 500));
        ar.setCreatedAt(LocalDateTime.now());
        // 不再保存到 analysis_results 表（该表已废弃）
        // return analysisResultRepository.save(ar);
        return ar;  // 仅返回内存对象
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    // 在 JSON 树中递归查找给定 keys 的第一个字符串值
    private String findFirstStringByKeys(JsonNode node, String[] keys) {
        if (node == null) return null;
        // 如果节点本身是对象，检查字段
        if (node.isObject()) {
            for (String key : keys) {
                JsonNode found = node.get(key);
                if (found != null && found.isTextual()) return found.asText();
            }
        }
        // 如果节点是数组或对象，递归遍历子节点
        Iterator<JsonNode> it = node.elements();
        while (it.hasNext()) {
            JsonNode child = it.next();
            String v = findFirstStringByKeys(child, keys);
            if (v != null) return v;
        }
        // 还要检查字段 names if object
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fname = fieldNames.next();
                JsonNode child = node.get(fname);
                String v = findFirstStringByKeys(child, keys);
                if (v != null) return v;
            }
        }
        return null;
    }    /**
     * 格式化 Kimi 的响应为标准格式
     * Kimi 应该直接返回 JSON 格式: { "summary": "...", "tableData": [...] }
     * 如果返回的是纯文本，则尽力解析
     */
    private String formatKimiResponse(String kimiRawResponse) {
        System.out.println("\n--- formatKimiResponse 开始处理 ---");
        try {
            if (kimiRawResponse == null || kimiRawResponse.trim().isEmpty()) {
                System.out.println("⚠️  输入为空，返回空响应");
                Map<String, Object> empty = new HashMap<>();
                empty.put("summary", "");
                empty.put("tableData", new ArrayList<>());
                return objectMapper.writeValueAsString(empty);
            }

            String trimmedResponse = kimiRawResponse.trim();
            System.out.println("原始响应长度: " + trimmedResponse.length() + " 字符");
            System.out.println("原始响应首字符: '" + trimmedResponse.charAt(0) + "'");
            
            // 尝试直接解析为 JSON
            try {
                JsonNode root = objectMapper.readTree(trimmedResponse);
                
                // 检查是否已经是标准格式
                if (root.isObject() && root.has("summary") && root.has("tableData")) {
                    System.out.println("✅ 已是标准 JSON 格式，直接返回");
                    // 验证 tableData 是数组
                    JsonNode tableDataNode = root.get("tableData");
                    if (tableDataNode.isArray()) {
                        System.out.println("✅ tableData 是数组，共 " + tableDataNode.size() + " 行");
                        return trimmedResponse;
                    }
                }
                
                // 如果是 JSON 但不是标准格式，尝试提取内容
                System.out.println("ℹ️  JSON 格式但非标准结构");
                String summary = extractSummaryFromJson(root);
                List<Map<String, Object>> tableData = extractTableDataFromJson(root);
                
                Map<String, Object> formatted = new HashMap<>();
                formatted.put("summary", summary);
                formatted.put("tableData", tableData);
                String result = objectMapper.writeValueAsString(formatted);
                System.out.println("✅ 格式化完成，tableData 行数: " + tableData.size());
                return result;
                
            } catch (Exception jsonEx) {
                System.out.println("⚠️  JSON 解析失败，尝试作为纯文本处理");
                // 如果是纯文本，返回为 summary
                Map<String, Object> result = new HashMap<>();
                result.put("summary", trimmedResponse);
                result.put("tableData", new ArrayList<>());
                String output = objectMapper.writeValueAsString(result);
                System.out.println("✅ 以纯文本格式返回");
                return output;
            }
            
        } catch (Exception e) {
            System.err.println("❌ 格式化异常: " + e.getMessage());
            e.printStackTrace();
            try {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("summary", kimiRawResponse != null ? kimiRawResponse.substring(0, Math.min(500, kimiRawResponse.length())) : "分析失败");
                fallback.put("tableData", new ArrayList<>());
                String result = objectMapper.writeValueAsString(fallback);
                System.err.println("⚠️  使用兜底方案返回");
                return result;
            } catch (Exception ex) {
                System.err.println("❌ 兜底方案也失败了");
                return "{\"summary\": \"" + (kimiRawResponse != null ? kimiRawResponse.substring(0, Math.min(200, kimiRawResponse.length())) : "error") + "\", \"tableData\": []}";
            }
        }
    }
    
    /**
     * 从 JSON 对象中提取 summary
     */
    private String extractSummaryFromJson(JsonNode node) {
        if (node.has("summary")) {
            return node.get("summary").asText();
        }
        if (node.has("content")) {
            return node.get("content").asText();
        }
        if (node.has("text")) {
            return node.get("text").asText();
        }
        return node.toString();
    }
    
    /**
     * 从 JSON 对象中提取 tableData 数组
     */
    private List<Map<String, Object>> extractTableDataFromJson(JsonNode node) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (node.has("tableData")) {
            JsonNode tableNode = node.get("tableData");
            if (tableNode.isArray()) {
                for (JsonNode row : tableNode) {
                    if (row.isObject()) {
                        Map<String, Object> rowMap = new HashMap<>();
                        row.fieldNames().forEachRemaining(key -> {
                            JsonNode value = row.get(key);
                            rowMap.put(key, value.asText());
                        });
                        result.add(rowMap);
                    }
                }
            }
        }
        
        return result;
    }
    /**
     * 从格式化的响应中提取摘要
     */
    private String extractSummaryFromResponse(String formattedResponse, int maxLength) {
        try {
            JsonNode root = objectMapper.readTree(formattedResponse);
            if (root.has("summary")) {
                String summary = root.get("summary").asText();
                return truncate(summary, maxLength);
            }
        } catch (Exception e) {
            log.warn("提取摘要失败: {}", e.getMessage());
        }
        return truncate(formattedResponse, maxLength);
    }
}
