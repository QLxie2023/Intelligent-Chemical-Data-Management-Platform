package chem_data_platform.demo.service;

import chem_data_platform.demo.entity.AnalysisResult;
import chem_data_platform.demo.repository.AnalysisResultRepository;
import chem_data_platform.demo.repository.ChemicalInfoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private final AnalysisResultRepository analysisResultRepository;
    private final ChemicalInfoRepository chemicalInfoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeterRegistry meterRegistry;

    @Value("${analysis.provider:qwen}")
    private String analysisProvider;

    @Autowired(required = false)
    private QwenService qwenService;

    public AnalysisService(AnalysisResultRepository analysisResultRepository,
                           ChemicalInfoRepository chemicalInfoRepository,
                           MeterRegistry meterRegistry) {
        this.analysisResultRepository = analysisResultRepository;
        this.chemicalInfoRepository = chemicalInfoRepository;
        this.meterRegistry = meterRegistry;
    }

    public AnalysisResult analyzeFile(Long projectId, Long fileId, File file) {
        if (qwenService != null && qwenService.isConfigured()) {
            return analyzeFileWithQwen(projectId, fileId, file);
        } else {
            throw new IllegalStateException("Qwen AI service not configured. Set qwen.api-key in application.properties");
        }
    }

    private AnalysisResult analyzeFileWithQwen(Long projectId, Long fileId, File file) {
        log.info("Analyzing file with Qwen: projectId={}, fileId={}", projectId, fileId);
        System.out.println("\n========== Analyzing with Qwen ==========");
        System.out.println("projectId: " + projectId + ", fileId: " + fileId);
        System.out.println("file: " + file.getAbsolutePath() + ", exists: " + file.exists());

        try {
            String prompt = QwenService.buildChemicalAnalysisPrompt();
            String rawResponse = qwenService.uploadAndAnalyze(file, prompt);

            log.info("Qwen analysis complete, response length: {}", rawResponse.length());
            System.out.println("========== [VERIFY] Qwen raw response (full) ==========");
            System.out.println(rawResponse);
            System.out.println("========== [VERIFY] End raw response ==========");

            String formattedResponse = ensureStandardFormat(rawResponse);

            System.out.println("========== [VERIFY] Formatted response (full) ==========");
            System.out.println(formattedResponse);
            System.out.println("========== [VERIFY] End formatted response ==========");

            AnalysisResult ar = new AnalysisResult();
            ar.setProjectId(projectId);
            ar.setFileId(fileId);
            ar.setRawResponse(formattedResponse);
            ar.setSummary(extractSummaryFromJson(formattedResponse));
            ar.setCreatedAt(LocalDateTime.now());

            meterRegistry.counter("analysis.requests", "provider", "qwen").increment();
            System.out.println("========== Qwen analysis complete ==========\n");
            return ar;

        } catch (Exception e) {
            log.error("Qwen analysis failed: {}", e.getMessage(), e);
            System.err.println("Qwen analysis error: " + e.getMessage());
            e.printStackTrace();
            AnalysisResult ar = new AnalysisResult();
            ar.setProjectId(projectId);
            ar.setFileId(fileId);
            ar.setRawResponse("qwen-error:" + e.getMessage());
            ar.setSummary("qwen-error:" + e.getMessage());
            ar.setCreatedAt(LocalDateTime.now());
            return ar;
        }
    }

    private String ensureStandardFormat(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                return objectMapper.writeValueAsString(Map.of(
                    "summary", "", "standardized_name", "N/A", "data_description", "N/A",
                    "keywords", List.of(), "tableData", List.of()
                ));
            }

            String cleaned = response.trim();
            if (cleaned.startsWith("```")) {
                int start = cleaned.indexOf('\n');
                int end = cleaned.lastIndexOf("```");
                if (start > 0 && end > start) {
                    cleaned = cleaned.substring(start + 1, end).trim();
                }
            }

            JsonNode root = objectMapper.readTree(cleaned);

            Map<String, Object> result = new HashMap<>();
            result.put("summary", root.has("summary") ? root.get("summary").asText() : "");
            result.put("standardized_name", root.has("standardized_name") ? root.get("standardized_name").asText() : "N/A");
            result.put("data_description", root.has("data_description") ? root.get("data_description").asText() : "N/A");

            if (root.has("keywords") && root.get("keywords").isArray()) {
                List<String> keywords = new ArrayList<>();
                root.get("keywords").forEach(k -> keywords.add(k.asText()));
                result.put("keywords", keywords);
            } else {
                result.put("keywords", List.of());
            }

            List<Map<String, Object>> tableData = new ArrayList<>();

            String[] arrayFields = {"tableData", "experiment_conditions", "results", "table_data_raw"};
            for (String field : arrayFields) {
                if (root.has(field) && root.get(field).isArray()) {
                    ArrayNode arr = (ArrayNode) root.get(field);
                    for (JsonNode item : arr) {
                        Map<String, Object> row = new HashMap<>();
                        item.fields().forEachRemaining(entry -> row.put(entry.getKey(), entry.getValue().asText()));
                        row.put("_category", field);
                        tableData.add(row);
                    }
                }
            }

            result.put("tableData", tableData);
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.warn("Failed to format response: {}", e.getMessage());
            try {
                return objectMapper.writeValueAsString(Map.of(
                    "summary", response.substring(0, Math.min(500, response.length())),
                    "standardized_name", "N/A", "data_description", "N/A",
                    "keywords", List.of(), "tableData", List.of()
                ));
            } catch (Exception ex) {
                return "{\"summary\":\"\",\"standardized_name\":\"N/A\",\"data_description\":\"N/A\",\"keywords\":[],\"tableData\":[]}";
            }
        }
    }

    private String extractSummaryFromJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("summary")) {
                String summary = root.get("summary").asText();
                return summary.length() <= 500 ? summary : summary.substring(0, 500);
            }
        } catch (Exception e) {
            log.warn("Failed to extract summary: {}", e.getMessage());
        }
        return json != null ? json.substring(0, Math.min(500, json.length())) : "";
    }

    public AnalysisResult analyzeImage(Long projectId, Long imageId, File image) {
        return analyzeFile(projectId, imageId, image);
    }
}
