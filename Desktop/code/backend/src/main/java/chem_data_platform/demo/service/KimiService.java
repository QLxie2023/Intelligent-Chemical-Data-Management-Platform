package chem_data_platform.demo.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

/**
 * Kimi AI 服务：
 * - 第一步 (Upload): 上传文件到 Kimi，获取 file_id
 * - 第二步 (Chat): 调用 Kimi 分析文件，获取结构化数据
 */
@Service
public class KimiService {

    private static final Logger log = LoggerFactory.getLogger(KimiService.class);

    @Value("${kimi.api-key:}")
    private String kimiApiKey;

    @Value("${kimi.api.upload-url:https://api.moonshot.cn/v1/files}")
    private String uploadUrl;

    @Value("${kimi.api.chat-url:https://api.moonshot.cn/v1/chat/completions}")
    private String chatUrl;

    @Value("${kimi.api.model:moonshot-v1-8k}")
    private String model;    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 应用启动时验证 Kimi API 配置
     */
    @PostConstruct
    public void init() {
        if (isConfigured()) {
            log.info("✅ Kimi API 已配置");
            log.info("   - 上传 URL: {}", uploadUrl);
            log.info("   - Chat URL: {}", chatUrl);
            log.info("   - 模型: {}", model);
            log.info("   - API Key: sk-***{}", kimiApiKey != null && kimiApiKey.length() > 10 
                ? kimiApiKey.substring(kimiApiKey.length() - 10) : "?");
        } else {
            log.warn("❌ ============================================");
            log.warn("❌ Kimi API 未配置或配置不正确！");
            log.warn("❌ 请在 application.properties 中添加：");
            log.warn("❌   kimi.api-key=sk-你的密钥");
            log.warn("❌   analysis.provider=kimi");
            log.warn("❌ ============================================");
        }
    }

    /**
     * 材料科学数据提取 Prompt - 热喷涂涂层实验数据（Markdown表格格式）
     */
    private static final String MATERIALS_SCIENCE_THERMAL_SPRAY_PROMPT = 
        "请扮演一位专业的材料科学数据助理。阅读我上传的这篇关于热喷涂 Al2O3-TiO2 或 Cr2O3-TiO2 涂层的论文。\n\n" +
        "你的任务是提取文中的实验数据，并整理成 Markdown 表格格式。\n\n" +
        "请重点关注以下字段（如果没有提及，请填 \"N/A\"；如果文中是范围值，请填平均值或保持范围格式）：\n" +
        "1. Material_Composition (涂层成分，如 Al2O3-13wt%TiO2)\n" +
        "2. Spray_Process (喷涂工艺，如 APS, HVOF)\n" +
        "3. Spray_Distance_mm (喷距)\n" +
        "4. Gun_Speed_mm_s (走枪速度)\n" +
        "5. Powder_Feed_Rate_g_min (送粉率)\n" +
        "6. Porosity_% (孔隙率)\n" +
        "7. Microhardness_GPa (硬度，如果是HV请注明)\n" +
        "8. Counterpart_Ball (对偶球材料，如 Steel, Si3N4)\n" +
        "9. Load_N (载荷)\n" +
        "10. Wear_Rate (磨损率，请统一单位并在备注中说明原文单位)\n\n" +
        "注意事项：\n" +
        "- 如果文中有对比不同参数（如不同 TiO2 含量或不同载荷）的多组实验，请提取每一组数据作为表格的一行。\n" +
        "- 重点检查文中出现的 \"Table\"（表格）部分。\n" +
        "- 如果数据在图片中无法读取，请告诉我\"图X包含相关数据\"。";    /**
     * 第一步：上传文件到 Kimi
     * @param file MultipartFile 文件
     * @return 返回 Kimi 返回的 file_id（例如 file-cnm...）
     * @throws IOException 文件读取异常
     */
    public String uploadFileToKimi(MultipartFile file) throws IOException {
        log.info("Uploading file to Kimi: {}", file.getOriginalFilename());
        System.out.println("\n========== 🔄 Kimi 文件上传 ==========");
        System.out.println("文件名: " + file.getOriginalFilename());
        System.out.println("大小: " + file.getSize() + " bytes");
        System.out.println("类型: " + file.getContentType());
        System.out.println("API Key: sk-***" + (kimiApiKey != null && kimiApiKey.length() > 10 ? 
            kimiApiKey.substring(kimiApiKey.length() - 10) : "?"));

        // 转换为临时 File 对象
        File tempFile = convertMultipartToFile(file);

        try {
            // 构建 multipart 请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            org.springframework.core.io.FileSystemResource fileResource = 
                new org.springframework.core.io.FileSystemResource(tempFile);
            body.add("file", fileResource);
            body.add("purpose", "file-extract");

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(kimiApiKey);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = 
                new HttpEntity<>(body, headers);

            // 发送请求
            System.out.println("🔄 发送请求到: " + uploadUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(
                uploadUrl, 
                requestEntity, 
                String.class
            );

            System.out.println("📡 HTTP 状态码: " + response.getStatusCode());
            log.debug("Kimi upload response: {}", response.getBody());
            System.out.println("📡 响应体: " + (response.getBody() != null ? 
                response.getBody().substring(0, Math.min(200, response.getBody().length())) : "null"));

            // 解析响应，提取 file_id
            String fileId = parseFileIdFromResponse(response.getBody());
            log.info("File uploaded successfully, file_id: {}", fileId);
            System.out.println("✅ 文件上传成功, Kimi file_id: " + fileId);
            System.out.println("========== ✅ Kimi 文件上传完成 ==========\n");
            return fileId;

        } catch (Exception e) {
            log.error("Failed to upload file to Kimi", e);
            System.err.println("❌ Kimi 上传失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload file to Kimi: " + e.getMessage(), e);
        } finally {
            // 删除临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 第二步：调用 Kimi Chat API，分析文件并获取结构化数据
     * @param fileId Kimi 返回的 file_id
     * @param prompt 自定义提示词（默认使用英文化学专家提示）
     * @return 返回 Kimi 的分析结果（JSON 字符串）
     */
    public String analyzeFileWithKimi(String fileId, String prompt) {
        log.info("Analyzing file with Kimi, file_id: {}", fileId);

        // 如果没有提供 prompt，使用默认的
        if (prompt == null || prompt.isEmpty()) {
            prompt = "Please summarize this document (within 200 words) and extract all experimental data "
                   + "(compounds, conditions, yields). Return the result strictly in JSON format with the following structure:\n"
                   + "{\n"
                   + "  \"summary\": \"...\",\n"
                   + "  \"compounds\": [{\"name\": \"\", \"formula\": \"\", \"cas\": \"\"}],\n"
                   + "  \"conditions\": {\"temperature\": \"\", \"pressure\": \"\", \"solvent\": \"\"},\n"
                   + "  \"yields\": [{\"product\": \"\", \"yield_percent\": \"\", \"unit\": \"\"}],\n"
                   + "  \"other_data\": {}\n"
                   + "}";
        }

        try {
            // 构建请求体
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);

            // 构建 messages 数组
            List<Map<String, String>> messages = new ArrayList<>();

            // 系统角色：声明为化学专家
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a chemistry expert. Please analyze the uploaded document carefully "
                    + "and extract experimental data in the specified JSON format.");
            messages.add(systemMessage);

            // 文件内容角色：包含 file_id
            Map<String, String> fileMessage = new HashMap<>();
            fileMessage.put("role", "system");
            fileMessage.put("content", fileId);
            messages.add(fileMessage);

            // 用户提示
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            payload.put("messages", messages);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(kimiApiKey);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                chatUrl,
                requestEntity,
                String.class
            );

            log.debug("Kimi chat response: {}", response.getBody());

            // 解析响应并提取内容
            String analysisResult = extractAnalysisResult(response.getBody());
            log.info("Analysis completed successfully");
            return analysisResult;

        } catch (Exception e) {
            log.error("Failed to analyze file with Kimi", e);
            throw new RuntimeException("Failed to analyze file with Kimi: " + e.getMessage(), e);
        }
    }

    /**
     * 一体化方法：上传文件并分析
     * @param file MultipartFile 文件
     * @param prompt 自定义提示词
     * @return 返回分析结果
     */
    public String uploadAndAnalyzeFile(MultipartFile file, String prompt) throws IOException {
        // 第一步：上传文件
        String fileId = uploadFileToKimi(file);

        // 第二步：分析文件
        return analyzeFileWithKimi(fileId, prompt);
    }    /**
     * 将 MultipartFile 转换为 File
     */
    private File convertMultipartToFile(MultipartFile multipartFile) throws IOException {
        // 使用 UUID 确保临时文件名唯一性，避免 FileAlreadyExistsException
        String uniqueFileName = "kimi_upload_" + UUID.randomUUID() + getFileExtension(multipartFile.getOriginalFilename());
        File tempFile = new File(System.getProperty("java.io.tmpdir"), uniqueFileName);
        multipartFile.transferTo(tempFile);
        return tempFile;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".bin";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 从 Kimi 上传响应中解析 file_id
     * 预期响应格式：
     * {
     *   "id": "file-cnm...",
     *   "object": "file",
     *   ...
     * }
     */
    private String parseFileIdFromResponse(String responseBody) throws IOException {
        if (responseBody == null || responseBody.isEmpty()) {
            throw new IllegalArgumentException("Empty response from Kimi upload API");
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode idNode = root.get("id");
        
        if (idNode == null) {
            log.error("Cannot find 'id' field in Kimi response: {}", responseBody);
            throw new IllegalArgumentException("Cannot find 'id' field in Kimi upload response");
        }

        return idNode.asText();
    }

    /**
     * 从 Kimi Chat 响应中提取分析结果
     * 预期响应格式：
     * {
     *   "choices": [
     *     {
     *       "message": {
     *         "content": "..."
     *       }
     *     }
     *   ]
     * }
     */
    private String extractAnalysisResult(String responseBody) throws IOException {
        if (responseBody == null || responseBody.isEmpty()) {
            throw new IllegalArgumentException("Empty response from Kimi chat API");
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choicesNode = root.get("choices");

        if (choicesNode == null || choicesNode.size() == 0) {
            log.error("Cannot find 'choices' in Kimi response: {}", responseBody);
            throw new IllegalArgumentException("Cannot find 'choices' in Kimi chat response");
        }

        JsonNode messageNode = choicesNode.get(0).get("message");
        if (messageNode == null) {
            log.error("Cannot find 'message' in Kimi response: {}", responseBody);
            throw new IllegalArgumentException("Cannot find 'message' in Kimi chat response");
        }

        JsonNode contentNode = messageNode.get("content");
        if (contentNode == null) {
            log.error("Cannot find 'content' in Kimi response: {}", responseBody);
            throw new IllegalArgumentException("Cannot find 'content' in Kimi chat response");
        }

        return contentNode.asText();
    }

    /**
     * 验证 API Key 是否已配置
     */
    public boolean isConfigured() {
        return kimiApiKey != null && !kimiApiKey.isEmpty() && !kimiApiKey.equals("sk-");
    }

    /**
     * 获取配置的模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 材料科学专用：上传论文并提取热喷涂涂层实验数据
     * @param file 论文文件（PDF、Word等）
     * @return 返回 Markdown 格式的实验数据表格
     * @throws IOException 文件读取异常
     */
    public String uploadAndExtractThermalSprayData(MultipartFile file) throws IOException {
        log.info("Starting thermal spray coating data extraction from: {}", file.getOriginalFilename());
        return uploadAndAnalyzeFile(file, MATERIALS_SCIENCE_THERMAL_SPRAY_PROMPT);
    }

    /**
     * 材料科学专用：使用已上传的文件 ID，提取热喷涂涂层实验数据
     * @param fileId Kimi 返回的 file_id
     * @return 返回 Markdown 格式的实验数据表格
     */
    public String extractThermalSprayDataFromFileId(String fileId) {
        log.info("Extracting thermal spray coating data from file_id: {}", fileId);
        return analyzeFileWithKimi(fileId, MATERIALS_SCIENCE_THERMAL_SPRAY_PROMPT);
    }
}