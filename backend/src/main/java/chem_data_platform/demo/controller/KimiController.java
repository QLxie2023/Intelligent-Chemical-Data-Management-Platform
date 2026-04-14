package chem_data_platform.demo.controller;

import chem_data_platform.demo.service.KimiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Kimi AI 分析控制器
 * 提供文件上传和智能分析的 API 端点
 */
@RestController
@RequestMapping("/api/kimi")
@Tag(name = "Kimi AI Analysis", description = "Kimi AI 文件分析接口")
public class KimiController {

    private static final Logger log = LoggerFactory.getLogger(KimiController.class);

    @Autowired
    private KimiService kimiService;

    /**
     * 获取 Kimi 配置状态
     */
    @GetMapping("/config/status")
    @Operation(summary = "Check Kimi configuration", description = "检查 Kimi API 是否已配置")
    public ResponseEntity<Map<String, Object>> checkStatus() {
        Map<String, Object> response = new HashMap<>();
        boolean configured = kimiService.isConfigured();
        response.put("configured", configured);
        response.put("model", kimiService.getModel());
        
        if (!configured) {
            response.put("warning", "Kimi API Key is not configured. Please set 'kimi.api-key' in application.properties");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 上传文件到 Kimi
     * @param file 要上传的文件（PDF、Word、TXT 等）
     * @return 包含 file_id 的响应
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload file to Kimi", description = "上传文件到 Kimi 获取 file_id")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Uploading file to Kimi: {}", file.getOriginalFilename());
            String fileId = kimiService.uploadFileToKimi(file);

            response.put("success", true);
            response.put("file_id", fileId);
            response.put("original_filename", file.getOriginalFilename());
            response.put("file_size", file.getSize());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            response.put("success", false);
            response.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 分析已上传的文件
     * @param fileId Kimi 返回的 file_id
     * @param prompt 自定义分析提示词（可选，如果不提供将使用默认）
     * @return 分析结果
     */
    @PostMapping("/analyze")
    @Operation(summary = "Analyze file with Kimi", description = "调用 Kimi 分析已上传的文件")
    public ResponseEntity<Map<String, Object>> analyzeFile(
            @RequestParam("file_id") String fileId,
            @RequestParam(value = "prompt", required = false) String prompt) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (fileId == null || fileId.isEmpty()) {
                response.put("success", false);
                response.put("error", "file_id is required");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Analyzing file with Kimi: {}", fileId);
            String analysisResult = kimiService.analyzeFileWithKimi(fileId, prompt);

            response.put("success", true);
            response.put("file_id", fileId);
            response.put("analysis_result", analysisResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to analyze file", e);
            response.put("success", false);
            response.put("error", "Analysis failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 一体化接口：上传文件并分析
     * @param file 要分析的文件
     * @param prompt 自定义分析提示词（可选）
     * @return 包含分析结果的响应
     */
    @PostMapping("/upload-and-analyze")
    @Operation(summary = "Upload and analyze file", description = "一步完成文件上传和分析")
    public ResponseEntity<Map<String, Object>> uploadAndAnalyzeFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", required = false) String prompt) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Uploading and analyzing file: {}", file.getOriginalFilename());
            String analysisResult = kimiService.uploadAndAnalyzeFile(file, prompt);

            response.put("success", true);
            response.put("original_filename", file.getOriginalFilename());
            response.put("file_size", file.getSize());
            response.put("analysis_result", analysisResult);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to upload and analyze file", e);
            response.put("success", false);
            response.put("error", "Operation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取默认的分析 Prompt
     */
    @GetMapping("/default-prompt")
    @Operation(summary = "Get default analysis prompt", description = "获取默认的分析提示词")
    public ResponseEntity<Map<String, String>> getDefaultPrompt() {
        Map<String, String> response = new HashMap<>();
        response.put("prompt", "Please summarize this document (within 200 words) and extract all experimental data "
                + "(compounds, conditions, yields). Return the result strictly in JSON format with the following structure:\n"
                + "{\n"
                + "  \"summary\": \"...\",\n"
                + "  \"compounds\": [{\"name\": \"\", \"formula\": \"\", \"cas\": \"\"}],\n"
                + "  \"conditions\": {\"temperature\": \"\", \"pressure\": \"\", \"solvent\": \"\"},\n"
                + "  \"yields\": [{\"product\": \"\", \"yield_percent\": \"\", \"unit\": \"\"}],\n"
                + "  \"other_data\": {}\n"
                + "}");
        return ResponseEntity.ok(response);
    }
}
