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
        systemMsg.put("content", "You are a professional document data extraction expert. Your task is to extract structured data from uploaded documents.\n\n" +
                "Core rules:\n" +
                "1. You must read and understand the document content first, then extract data based on the actual content.\n" +
                "2. summary and standardized_name must reflect the true content of the document, do not fill N/A.\n" +
                "3. For experiment conditions, results, etc., extract if present in the document; otherwise use empty array [].\n" +
                "4. All numerical values must match the original document, preserving original precision and units.\n" +
                "5. You must output strict JSON format, do not include markdown syntax or extra explanation text.\n" +
                "6. Every field must be filled, do not omit any field.\n" +
                "7. Absolutely do not fabricate data. If you cannot read the document content, return {\"error\": \"Unable to read document content\"}.\n" +
                "8. All output content must be in English.");
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
        return "Please read the uploaded document carefully and extract structured data from it.\n\n" +
                "Important rules:\n" +
                "- You must fill every field based on the actual content of the document.\n" +
                "- summary and standardized_name must reflect the true content of the document, never fill \"N/A\".\n" +
                "- If the document is an experiment report, extract experiment conditions and results; for other types, extract key information as results.\n" +
                "- If the document contains tables, extract them into table_data_raw preserving the original structure.\n" +
                "- keywords must contain 3-8 key terms extracted from the document, cannot be empty.\n" +
                "- All output content must be in English.\n\n" +
                "Please return results in the following JSON format strictly (do not include markdown markers):\n\n" +
                "{\n" +
                "  \"standardized_name\": \"Standardized name of the document, format: DocumentType_Topic_KeyFeature (e.g., Competition_Directory_NationalStudentCompetition_2023, or Resume_SoftwareEngineering_GraduateApplication)\",\n" +
                "  \"summary\": \"Summary based on the original document (150-300 words), must cover the core content including key data and findings.\",\n" +
                "  \"data_description\": \"Overall description of the data, including: document source, data type (qualitative/quantitative), data content overview\",\n" +
                "  \"experiment_conditions\": [\n" +
                "    {\"parameter\": \"Parameter name\", \"value\": \"Value extracted from document\", \"unit\": \"Unit\", \"remarks\": \"Remarks\"}\n" +
                "  ],\n" +
                "  \"results\": [\n" +
                "    {\"parameter\": \"Metric name\", \"value\": \"Value extracted from document\", \"unit\": \"Unit\", \"remarks\": \"Remarks\"}\n" +
                "  ],\n" +
                "  \"keywords\": [\"3-8 key terms extracted from the document\"],\n" +
                "  \"table_data_raw\": [\n" +
                "    {\"row_label\": \"Row label\", \"col1\": \"Value 1\", \"col2\": \"Value 2\"}\n" +
                "  ]\n" +
                "}\n\n" +
                "Field instructions:\n" +
                "- standardized_name: Must be named based on document content, do not fill N/A\n" +
                "- summary: Must summarize the core content of the document, do not fill N/A\n" +
                "- data_description: Describe data characteristics in the document, do not fill N/A\n" +
                "- experiment_conditions: Extract if the document contains experiment conditions; otherwise use empty array []\n" +
                "- results: Extract key data metrics from the document; lists, rankings, statistics also go here\n" +
                "- keywords: Must extract 3-8 keywords from professional terms or core concepts in the document\n" +
                "- table_data_raw: Extract table data preserving original structure if present; otherwise use empty array []";
    }
}
