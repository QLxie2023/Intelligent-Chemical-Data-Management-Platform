package chem_data_platform.demo.controller;

import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;

import chem_data_platform.demo.dto.FileUploadResponseDTO;
import chem_data_platform.demo.dto.ImageUploadResponseDTO;
import chem_data_platform.demo.service.ProjectService;
import chem_data_platform.demo.service.AnalysisService;
import chem_data_platform.demo.service.DataProcessingService;
import chem_data_platform.demo.entity.FileInfo;
import chem_data_platform.demo.repository.FileInfoRepository;
import chem_data_platform.demo.utils.JwtUtil;
import chem_data_platform.demo.vo.ApiResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1")
public class DataUploadController {    @Autowired
    private ProjectService projectService;    
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AnalysisService analysisService;
    
    @Autowired
    private DataProcessingService dataProcessingService;
    
    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Autowired
    private ApplicationContext applicationContext;  // 用于获取代理对象以启用 @Transactional
    
    @Value("${upload.base-path:C:/uploads/chem_data_platform}")
    private String uploadBasePath;/**
     * 分析文件
     * POST /api/v1/files/{fileId}/analysis
     * 触发异步文件分析任务
     */
    @PostMapping("/files/{fileId}/analysis")
    public ResponseEntity<ApiResponse<?>> analyzeFile(
            @PathVariable Long fileId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("无效的认证令牌"));
            }

            // 从数据库查询文件信息
            Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
            if (!fileOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("文件不存在"));
            }

            FileInfo fileInfo = fileOpt.get();
            
            // 更新文件分析状态为 PROCESSING
            projectService.updateFileAnalysisStatus(fileId, "PROCESSING");

            // 异步触发文件分析任务
            // 构造文件路径：uploadBasePath/projects/{projectId}/files/{fileName}
            String filePath = uploadBasePath + "/projects/" + fileInfo.getProjectId() + "/files/" + fileInfo.getFileName();
            File file = new File(filePath);
            
            // 在后台线程中执行分析（不阻塞 HTTP 响应）
            performAnalysisAsync(fileInfo.getProjectId(), fileId, file);

            Map<String, Object> result = new HashMap<>();
            result.put("fileId", fileId);
            result.put("status", "PROCESSING");
            result.put("message", "文件分析已启动，AI 正在读取文献并提取特征...");

            return ResponseEntity.ok(ApiResponse.success("文件分析请求已提交", result));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest(e.getMessage()));
        }
    }    /**
     * 异步执行文件分析
     * 在后台线程中调用 AnalysisService 进行文件分析
     * 关键：使用 ApplicationContext 获取代理对象，确保 @Transactional 生效
     */
    @Async("asyncExecutor")
    public void performAnalysisAsync(Long projectId, Long fileId, File file) {
        try {
            System.out.println("\n========== 🔄 异步分析任务启动 ==========");
            System.out.println("线程: " + Thread.currentThread().getName());
            System.out.println("projectId: " + projectId);
            System.out.println("fileId: " + fileId);
            System.out.println("文件路径: " + file.getAbsolutePath());
            System.out.println("文件存在: " + file.exists());
            System.out.println("文件大小: " + (file.exists() ? file.length() : "N/A") + " bytes");
            
            if (!file.exists()) {
                String errorMsg = "文件不存在: " + file.getAbsolutePath();
                System.err.println("❌ " + errorMsg);
                // 通过代理对象调用，确保 @Transactional 生效
                ProjectService proxyService = applicationContext.getBean(ProjectService.class);
                proxyService.saveFileAnalysisError(fileId, errorMsg);
                return;
            }
            
            System.out.println("🔄 调用 AnalysisService.analyzeFile()...");
            // 调用 AnalysisService 执行分析（支持 Kimi）
            var analysisResult = analysisService.analyzeFile(projectId, fileId, file);
            
            if (analysisResult != null && analysisResult.getRawResponse() != null) {
                // 分析成功，保存结果到数据库
                String analysisJson = analysisResult.getRawResponse();
                System.out.println("✅ 分析结果获取成功，长度: " + analysisJson.length() + " chars");
                System.out.println("✅ 开始保存分析结果到数据库...");
                
                try {
                    // 关键修改：通过代理对象调用，确保 @Transactional 生效
                    ProjectService proxyService = applicationContext.getBean(ProjectService.class);
                    proxyService.saveFileAnalysisResult(fileId, analysisJson);
                    System.out.println("✅ 分析结果已保存到数据库，fileId=" + fileId);
                    System.out.println("✅ 分析状态已更新为 COMPLETED");
                } catch (Exception dbEx) {
                    System.err.println("❌ 数据库保存失败: " + dbEx.getMessage());
                    dbEx.printStackTrace();
                    ProjectService proxyService = applicationContext.getBean(ProjectService.class);
                    proxyService.saveFileAnalysisError(fileId, "数据库保存失败: " + dbEx.getMessage());
                    return;
                }
                
                System.out.println("✅ 文件分析完成: fileId=" + fileId);
                System.out.println("========== ✅ 异步分析任务完成 ==========\n");
            } else {
                // 分析结果为空
                String errorMsg = "分析结果为空或 RawResponse 为 null";
                System.err.println("❌ " + errorMsg);
                ProjectService proxyService = applicationContext.getBean(ProjectService.class);
                proxyService.saveFileAnalysisError(fileId, errorMsg);
            }
        } catch (RuntimeException e) {
            // 分析失败，记录错误信息
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("❌ 异步分析异常: " + errorMsg);
            e.printStackTrace();
            ProjectService proxyService = applicationContext.getBean(ProjectService.class);
            proxyService.saveFileAnalysisError(fileId, errorMsg);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("❌ 异步分析异常: " + errorMsg);
            e.printStackTrace();
            ProjectService proxyService = applicationContext.getBean(ProjectService.class);
            proxyService.saveFileAnalysisError(fileId, errorMsg);
        }
    }

    /**
     * 同步保存分析结果到数据库
     * 由异步线程调用，但实际执行在主线程中（确保事务生效）
     */
    private void saveAnalysisResultSync(Long fileId, String analysisJson) {
        projectService.saveFileAnalysisResult(fileId, analysisJson);
    }

    /**
     * 同步保存分析错误到数据库
     * 由异步线程调用，但实际执行在主线程中（确保事务生效）
     */
    private void saveAnalysisErrorSync(Long fileId, String errorMsg) {
        projectService.saveFileAnalysisError(fileId, errorMsg);
    }/**
     * 获取文件分析结果
     * GET /api/v1/files/{fileId}/analysis
     * 前端轮询此接口查询分析进度
     */
    @GetMapping("/files/{fileId}/analysis")
    public ResponseEntity<ApiResponse<?>> getAnalysisResult(
            @PathVariable Long fileId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("无效的认证令牌"));
            }

            // 从数据库查询文件分析状态
            Map<String, Object> analysisData = projectService.getFileAnalysisStatus(fileId);

            if (analysisData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("文件分析记录不存在"));
            }

            String status = (String) analysisData.get("status");
            Map<String, Object> result = new HashMap<>();
            result.put("fileId", fileId);
            result.put("status", status);

            // 如果分析完成，返回分析数据（19个机器学习特征）
            if ("COMPLETED".equals(status)) {
                result.put("summary", analysisData.get("summary"));
                result.put("tableData", analysisData.get("tableData"));
                return ResponseEntity.ok(ApiResponse.success("分析完成", result));
            }
            // 如果分析中，返回处理中状态
            if ("PROCESSING".equals(status)) {
                result.put("summary", null);
                result.put("tableData", null);
                return ResponseEntity.ok(ApiResponse.success("AI 正在读取文献并提取特征...", result));
            }
            // 如果分析失败
            if ("FAILED".equals(status)) {
                result.put("errorReason", analysisData.get("errorReason"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(500, "分析失败", result));
            }

            return ResponseEntity.ok(ApiResponse.success("获取分析结果成功", result));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest(e.getMessage()));
        }
    }
    
    /**
     * 下载文件分析结果
     * GET /api/v1/files/{fileId}/analysis/download
     */
    @GetMapping("/files/{fileId}/analysis/download")
    public ResponseEntity<?> downloadAnalysisResult(
            @PathVariable Long fileId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("无效的认证令牌"));
            }

            // 从数据库查询文件分析结果
            Map<String, Object> analysisData = projectService.getFileAnalysisStatus(fileId);

            if (analysisData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("文件分析记录不存在"));
            }

            String status = (String) analysisData.get("status");
            
            // 只有完成状态才能下载
            if ("COMPLETED".equals(status)) {
                Map<String, Object> downloadData = new HashMap<>();
                downloadData.put("fileId", fileId);
                downloadData.put("status", "COMPLETED");
                downloadData.put("summary", analysisData.get("summary"));
                downloadData.put("tableData", analysisData.get("tableData"));
                downloadData.put("analysisData", analysisData.get("analysisData"));
                
                return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"analysis_" + fileId + ".json\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(downloadData);
            } else if ("PROCESSING".equals(status)) {
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.fail(202, "分析仍在进行中，请稍后再试", null));
            } else if ("FAILED".equals(status)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(500, "分析失败: " + analysisData.get("errorReason"), null));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest("文件还未开始分析，请先请求分析"));
            }        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("下载失败: " + e.getMessage()));
        }
    }

    /**
     * 导出分析结果为 Excel
     * GET /api/v1/files/{fileId}/analysis/export-excel
     * 将 Kimi 返回的 JSON 数据转换为 Excel 文件供下载
     */
    @GetMapping("/files/{fileId}/analysis/export-excel")
    public ResponseEntity<?> exportAnalysisToExcel(
            @PathVariable Long fileId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("无效的认证令牌"));
            }

            // 从数据库查询文件分析结果
            Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
            if (!fileOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("文件不存在"));
            }

            FileInfo fileInfo = fileOpt.get();
            String analysisStatus = fileInfo.getAnalysisStatus();
            String analysisData = fileInfo.getAnalysisData();

            // 只有完成状态才能导出
            if (analysisStatus == null || !"COMPLETED".equals(analysisStatus)) {
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.fail(202, "分析尚未完成，状态: " + analysisStatus, null));
            }

            if (analysisData == null || analysisData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest("分析数据为空"));
            }

            // 生成 Excel 文件
            String excelFileName = "analysis_" + fileId + "_" + System.currentTimeMillis() + ".xlsx";
            String excelPath = uploadBasePath + "/exports/" + excelFileName;
            
            // 调用数据处理服务生成 Excel
            String generatedPath = dataProcessingService.processAndGenerateExcel(analysisData, excelPath);
            
            if (generatedPath == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Excel 生成失败"));
            }

            // 读取 Excel 文件内容
            Path filePath = Paths.get(generatedPath);
            byte[] fileContent = Files.readAllBytes(filePath);

            // 返回 Excel 文件供下载
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + excelFileName + "\"")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("X-File-Name", excelFileName)
                .body(fileContent);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("导出 Excel 失败: " + e.getMessage()));
        }
    }

    @PostMapping("/projects/{projectId}/files")
    public ResponseEntity<ApiResponse<FileUploadResponseDTO>> uploadFile(
            @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            // 权限检查：当前实现仅检查项目是否存在与上传所有者关系
            String username = jwtUtil.getUsernameFromToken(token);
            if (!projectService.hasProjectPermission(projectId, username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.forbidden("无上传权限"));
            }

            FileUploadResponseDTO dto = projectService.uploadFile(projectId, file, username);
            return ResponseEntity.ok(ApiResponse.success("文件上传并触发分析（可能异步）", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.serverError("上传失败: " + e.getMessage()));
        }
    }

    @PostMapping("/projects/{projectId}/images")
    public ResponseEntity<ApiResponse<ImageUploadResponseDTO>> uploadImage(
            @PathVariable Long projectId,
            @RequestPart("image") MultipartFile image,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String username = jwtUtil.getUsernameFromToken(token);
            if (!projectService.hasProjectPermission(projectId, username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.forbidden("无上传权限"));
            }

            ImageUploadResponseDTO dto = projectService.uploadImage(projectId, image, username);
            return ResponseEntity.ok(ApiResponse.success("图片上传并触发分析（可能异步）", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.serverError("上传失败: " + e.getMessage()));
        }
    }

    /**
     * 删除文件
     * DELETE /api/v1/files/{fileId}
     */
    @PostMapping("/files/{fileId}/delete")
    public ResponseEntity<ApiResponse<?>> deleteFile(
            @PathVariable Long fileId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.unauthorized("无效的认证令牌"));
            }

            String username = jwtUtil.getUsernameFromToken(token);
            projectService.deleteFile(fileId, username);

            return ResponseEntity.ok(ApiResponse.success("文件删除成功", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.serverError("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 删除图片
     * DELETE /api/v1/images/{imageId}
     */
    @PostMapping("/images/{imageId}/delete")
    public ResponseEntity<ApiResponse<?>> deleteImage(
            @PathVariable Long imageId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.unauthorized("无效的认证令牌"));
            }

            String username = jwtUtil.getUsernameFromToken(token);
            projectService.deleteImage(imageId, username);

            return ResponseEntity.ok(ApiResponse.success("图片删除成功", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.serverError("删除失败: " + e.getMessage()));
        }
    }
}