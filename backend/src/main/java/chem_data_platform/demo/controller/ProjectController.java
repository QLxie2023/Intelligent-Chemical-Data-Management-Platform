package chem_data_platform.demo.controller;

import chem_data_platform.demo.dto.CreateProjectDTO;
import chem_data_platform.demo.dto.ProjectResponseDTO;
import chem_data_platform.demo.dto.ProjectDetailDTO;
import chem_data_platform.demo.dto.FileInfoDTO;
import chem_data_platform.demo.dto.ImageInfoDTO;
import chem_data_platform.demo.service.ProjectService;
import chem_data_platform.demo.utils.JwtUtil;
import chem_data_platform.demo.vo.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建新项目
     * POST /api/v1/projects
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponseDTO>> createProject(
            @Valid @RequestBody CreateProjectDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Authorization header basic validation
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            // 创建项目
            ProjectResponseDTO project = projectService.createProject(dto, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("项目创建成功！", project));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("项目创建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取项目列表
     * GET /api/v1/projects
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponseDTO>>> getProjects(
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            // 获取用户项目列表（包含用户所有者的项目和所有公开项目）
            List<ProjectResponseDTO> projects = projectService.getUserProjects(username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Success", projects));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("获取项目列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户所有者的私有项目列表
     * GET /api/v1/projects/private
     */
    @GetMapping("/private")
    public ResponseEntity<ApiResponse<List<ProjectResponseDTO>>> getUserPrivateProjects(
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            List<ProjectResponseDTO> projects = projectService.getUserPrivateProjects(username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("用户私有项目列表", projects));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("获取私有项目列表失败: " + e.getMessage()));
        }
    }    /**
     * 搜索项目
     * GET /api/v1/projects/search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProjectResponseDTO>>> searchProjects(
            @RequestParam("keyword") String keyword,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            List<ProjectResponseDTO> projects = projectService.searchProjects(keyword, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("搜索结果", projects));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("搜索项目失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有公开项目列表
     * GET /api/v1/projects/public
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ProjectResponseDTO>>> getPublicProjects() {
        try {
            List<ProjectResponseDTO> projects = projectService.getPublicProjects();

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("所有公开项目列表", projects));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("获取公开项目列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取项目详情
     * GET /api/v1/projects/{projectId}
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDetailDTO>> getProjectDetail(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            ProjectDetailDTO project = projectService.getProjectDetail(projectId, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("项目详情", project));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("获取项目详情失败: " + e.getMessage()));
        }
    }

    /**
     * 获取项目文件列表
     * GET /api/v1/projects/{projectId}/files
     */
    @GetMapping("/{projectId}/files")
    public ResponseEntity<ApiResponse<List<FileInfoDTO>>> getProjectFiles(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            List<FileInfoDTO> files = projectService.getProjectFiles(projectId, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("项目文件列表", files));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("获取项目文件列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取项目图片列表
     * GET /api/v1/projects/{projectId}/images
     */
    @GetMapping("/{projectId}/images")
    public ResponseEntity<ApiResponse<List<ImageInfoDTO>>> getProjectImages(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("未提供有效的认证令牌"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            List<ImageInfoDTO> images = projectService.getProjectImages(projectId, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("项目图片列表", images));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("获取项目图片列表失败: " + e.getMessage()));
        }
    }

    // File/image upload endpoints moved to DataUploadController to avoid duplicate mappings.
}