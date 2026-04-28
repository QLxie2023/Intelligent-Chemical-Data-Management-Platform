package chem_data_platform.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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

@Service
public class QwenService {

    private static final Logger log = LoggerFactory.getLogger(QwenService.class);

    @Value("${qwen.api-key:}")
    private String apiKey;

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${qwen.model:qwen3.6-flash}")
    private String model;

    @Value("${qwen.file-model:qwen-long}")
    private String fileModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            log.info("Qwen API configured: base_url={}, model={}, file_model={}", baseUrl, model, fileModel);
        } else {
            log.warn("Qwen API NOT configured. Set qwen.api-key in application.properties");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String uploadFile(File file) throws IOException {
        String url = baseUrl + "/files";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("purpose", "file-extract");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        System.out.println("[DEBUG] File upload response: " + response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        String fileId = root.get("id").asText();
        String status = root.has("status") ? root.get("status").asText() : "unknown";
        log.info("File uploaded to Qwen: fileId={}, status={}", fileId, status);
        System.out.println("[DEBUG] fileId=" + fileId + ", initial_status=" + status);

        int retries = 0;
        while (!"processed".equals(status) && retries < 30) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            status = checkFileStatus(fileId);
            retries++;
            log.info("File status check: fileId={}, status={}, attempt={}", fileId, status, retries);
            System.out.println("[DEBUG] Polling: fileId=" + fileId + ", status=" + status + ", attempt=" + retries);
        }

        if (!"processed".equals(status)) {
            log.warn("File may not be fully processed: fileId={}, status={}", fileId, status);
            System.err.println("[WARN] File NOT processed after " + retries + " attempts! status=" + status);
        } else {
            System.out.println("[DEBUG] File processed successfully: fileId=" + fileId);
        }

        return fileId;
    }

    public String uploadFile(MultipartFile multipartFile) throws IOException {
        File tempFile = File.createTempFile("qwen_upload_", "_" + multipartFile.getOriginalFilename());
        try {
            multipartFile.transferTo(tempFile);
            return uploadFile(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    private String checkFileStatus(String fileId) {
        try {
            String url = baseUrl + "/files/" + fileId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, requestEntity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.has("status") ? root.get("status").asText() : "unknown";
        } catch (Exception e) {
            log.warn("Failed to check file status: {}", e.getMessage());
            return "unknown";
        }
    }

    public String analyzeWithFileId(String fileId, String prompt) throws IOException {
        String url = baseUrl + "/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个专业的文档数据提取专家。你的任务是从用户上传的文档中提取结构化数据。\n\n" +
                "核心规则：\n" +
                "1. 你必须先阅读并理解文档内容，然后基于文档实际内容提取数据。\n" +
                "2. summary 和 standardized_name 必须反映文档的真实内容，不要填 N/A。\n" +
                "3. 对于实验条件、结果等字段，如果文档中有相关数据则提取，没有则填空数组 []。\n" +
                "4. 所有数值必须与文档原文一致，保留原始精度和单位。\n" +
                "5. 你必须输出严格的JSON格式，不要包含markdown语法或额外说明文字。\n" +
                "6. 每个字段都必须填写，不能省略任何字段。\n" +
                "7. 绝对不允许编造数据。如果无法读取文档内容，请返回 {\"error\": \"无法读取文档内容\"}。");
        messages.add(systemMsg);

        Map<String, String> fileRefMsg = new HashMap<>();
        fileRefMsg.put("role", "system");
        fileRefMsg.put("content", "fileid://" + fileId);
        messages.add(fileRefMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", fileModel);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.1);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        System.out.println("[DEBUG] ====== Qwen API Request ======");
        System.out.println("[DEBUG] URL: " + url);
        System.out.println("[DEBUG] Model: " + fileModel);
        System.out.println("[DEBUG] FileId reference: fileid://" + fileId);
        System.out.println("[DEBUG] Messages count: " + messages.size());
        System.out.println("[DEBUG] System msg 1 (role def): " + systemMsg.get("content").substring(0, Math.min(100, systemMsg.get("content").length())) + "...");
        System.out.println("[DEBUG] System msg 2 (file ref): " + fileRefMsg.get("content"));
        System.out.println("[DEBUG] User msg (prompt): " + prompt.substring(0, Math.min(200, prompt.length())) + "...");
        System.out.println("[DEBUG] Full request body length: " + jsonBody.length());
        System.out.println("[DEBUG] ====== End Request Debug ======");

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

        log.info("Calling Qwen chat API: model={}, fileId={}", fileModel, fileId);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, requestEntity, String.class);
        } catch (Exception e) {
            System.err.println("[ERROR] Qwen API call failed: " + e.getMessage());
            throw e;
        }

        System.out.println("[DEBUG] ====== Qwen API Response ======");
        System.out.println("[DEBUG] Status code: " + response.getStatusCode());
        String responseBody = response.getBody();
        System.out.println("[DEBUG] Response length: " + (responseBody != null ? responseBody.length() : 0));

        JsonNode root = objectMapper.readTree(responseBody);

        if (root.has("error")) {
            System.err.println("[ERROR] Qwen API returned error: " + root.get("error"));
        }

        if (root.has("usage")) {
            JsonNode usage = root.get("usage");
            int promptTokens = usage.get("prompt_tokens").asInt();
            System.out.println("[DEBUG] Token usage - prompt: " + promptTokens +
                    ", completion: " + usage.get("completion_tokens").asText() +
                    ", total: " + usage.get("total_tokens").asText());

            if (promptTokens < 200) {
                System.err.println("[CRITICAL] prompt_tokens=" + promptTokens + " is too low! File content was NOT read by Qwen!");
                System.err.println("[CRITICAL] This means Qwen is generating HALLUCINATED data without reading the actual file!");
                throw new IOException("Qwen failed to read file content (prompt_tokens=" + promptTokens + " is too low). File may not be properly processed on Bailian platform.");
            } else {
                System.out.println("[VERIFY] prompt_tokens=" + promptTokens + " looks reasonable, file content was likely read.");
            }
        } else {
            System.err.println("[WARN] No usage info in response, cannot verify if file was read.");
        }

        String content = root.at("/choices/0/message/content").asText();
        System.out.println("[DEBUG] Response content length: " + content.length());
        System.out.println("[DEBUG] Response content (first 500 chars): " + content.substring(0, Math.min(500, content.length())));
        System.out.println("[DEBUG] ====== End Response Debug ======");

        log.info("Qwen response received, length={}", content.length());
        return content;
    }

    public String uploadAndAnalyze(File file, String prompt) throws IOException {
        System.out.println("========== [VERIFY-STEP1] Uploading file to Bailian platform ==========");
        System.out.println("File: " + file.getAbsolutePath() + ", size: " + file.length() + " bytes");
        String fileId = uploadFile(file);
        System.out.println("========== [VERIFY-STEP1] Upload complete, fileId=" + fileId + " ==========");

        System.out.println("========== [VERIFY-STEP2] Calling Qwen chat API with fileId ==========");
        String result = analyzeWithFileId(fileId, prompt);
        System.out.println("========== [VERIFY-STEP2] Qwen response received, length=" + result.length() + " ==========");
        return result;
    }

    public String uploadAndAnalyze(MultipartFile multipartFile, String prompt) throws IOException {
        File tempFile = File.createTempFile("qwen_upload_", "_" + multipartFile.getOriginalFilename());
        try {
            multipartFile.transferTo(tempFile);
            return uploadAndAnalyze(tempFile, prompt);
        } finally {
            tempFile.delete();
        }
    }

    public static String buildChemicalAnalysisPrompt() {
        return "请仔细阅读上传的文档，从中提取结构化数据。\n\n" +
                "重要规则：\n" +
                "- 你必须基于文档的实际内容来填写每个字段。\n" +
                "- summary 和 standardized_name 必须反映文档的真实内容，绝对不能填 \"N/A\"。\n" +
                "- 如果文档是实验报告，提取实验条件和结果数据；如果是其他类型文档，提取文档中的关键信息作为 results。\n" +
                "- 如果文档中有表格，按原始结构提取到 table_data_raw。\n" +
                "- keywords 必须从文档中提取3-8个关键术语，不能为空。\n\n" +
                "请严格按照以下JSON格式返回结果（不要包含markdown标记）：\n\n" +
                "{\n" +
                "  \"standardized_name\": \"文档的规范化名称，格式：文档类型_主题_关键特征（如：竞赛目录_全国大学生竞赛_2023年度，或：简历_软件工程_保研申请）\",\n" +
                "  \"summary\": \"基于文档原文的摘要（150-300字），必须概括文档的核心内容，包含关键数据和发现。\",\n" +
                "  \"data_description\": \"数据的整体说明，包括：文档来源、数据类型（定性/定量）、数据内容概述\",\n" +
                "  \"experiment_conditions\": [\n" +
                "    {\"parameter\": \"参数名\", \"value\": \"从文档提取的数值\", \"unit\": \"单位\", \"remarks\": \"备注\"}\n" +
                "  ],\n" +
                "  \"results\": [\n" +
                "    {\"parameter\": \"指标名\", \"value\": \"从文档提取的数值\", \"unit\": \"单位\", \"remarks\": \"备注\"}\n" +
                "  ],\n" +
                "  \"keywords\": [\"从文档中提取的3-8个关键术语\"],\n" +
                "  \"table_data_raw\": [\n" +
                "    {\"row_label\": \"行标签\", \"col1\": \"值1\", \"col2\": \"值2\"}\n" +
                "  ]\n" +
                "}\n\n" +
                "字段填写说明：\n" +
                "- standardized_name: 必须根据文档内容命名，不要填 N/A\n" +
                "- summary: 必须概括文档核心内容，不要填 N/A\n" +
                "- data_description: 描述文档中的数据特征，不要填 N/A\n" +
                "- experiment_conditions: 如果文档包含实验条件则提取，否则填空数组 []\n" +
                "- results: 提取文档中的关键数据指标，如果文档有列表、排名、统计数据等也放在这里\n" +
                "- keywords: 必须提取3-8个关键词，来自文档中的专业术语或核心概念\n" +
                "- table_data_raw: 如果文档中有表格数据，按原始表格结构提取；没有则填空数组 []";
    }
}
