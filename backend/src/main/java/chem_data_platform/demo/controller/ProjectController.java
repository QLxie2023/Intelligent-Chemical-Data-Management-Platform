package chem_data_platform.demo.controller;

import chem_data_platform.demo.dto.CreateProjectDTO;
import chem_data_platform.demo.dto.ProjectResponseDTO;
import chem_data_platform.demo.dto.ProjectDetailDTO;
import chem_data_platform.demo.dto.FileInfoDTO;
import chem_data_platform.demo.dto.ImageInfoDTO;
import chem_data_platform.demo.dto.SearchResultDTO;
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
     * Create a new project
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
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            // Create project
            ProjectResponseDTO project = projectService.createProject(dto, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Project created successfully!", project));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Project creation failed: " + e.getMessage()));
        }
    }

    /**
     * Get project list
     * GET /api/v1/projects
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponseDTO>>> getProjects(
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            // Get the user's project list, including owned projects and all public projects
            List<ProjectResponseDTO> projects = projectService.getUserProjects(username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Success", projects));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Failed to get project list: " + e.getMessage()));
        }
    }

    /**
     * Get private projects owned by the user
     * GET /api/v1/projects/private
     */
    @GetMapping("/private")
    public ResponseEntity<ApiResponse<List<ProjectResponseDTO>>> getUserPrivateProjects(
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            List<ProjectResponseDTO> projects = projectService.getUserPrivateProjects(username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("User private project list", projects));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Failed to get private project list: " + e.getMessage()));
        }
    }    /**
     * Search projects
     * GET /api/v1/projects/search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SearchResultDTO>>> searchProjects(
            @RequestParam("keyword") String keyword,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            List<SearchResultDTO> results = projectService.searchByKeyword(keyword, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Search results", results));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Project search failed: " + e.getMessage()));
        }
    }

    /**
     * Get all public projects
     * GET /api/v1/projects/public
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ProjectResponseDTO>>> getPublicProjects() {
        try {
            List<ProjectResponseDTO> projects = projectService.getPublicProjects();

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("All public project list", projects));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Failed to get public project list: " + e.getMessage()));
        }
    }

    /**
     * Get project details
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
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            ProjectDetailDTO project = projectService.getProjectDetail(projectId, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Project details", project));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Failed to get project details: " + e.getMessage()));
        }
    }

    /**
     * Get project file list
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
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            List<FileInfoDTO> files = projectService.getProjectFiles(projectId, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Project file list", files));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Failed to get project file list: " + e.getMessage()));
        }
    }

    /**
     * Get project image list
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
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            List<ImageInfoDTO> images = projectService.getProjectImages(projectId, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Project image list", images));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Failed to get project image list: " + e.getMessage()));
        }
    }

    // File/image upload endpoints moved to DataUploadController to avoid duplicate mappings.

    /**
     * Delete project
     * POST /api/v1/projects/{projectId}/delete
     */
    @PostMapping("/{projectId}/delete")
    public ResponseEntity<ApiResponse<?>> deleteProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Missing a valid authentication token"));
            }
            String username = jwtUtil.getUsernameFromToken(token);

            projectService.deleteProject(projectId, username);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Project deleted successfully!", null));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Project deletion failed: " + e.getMessage()));
        }
    }
}