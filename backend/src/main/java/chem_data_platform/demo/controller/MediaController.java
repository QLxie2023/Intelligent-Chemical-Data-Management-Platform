package chem_data_platform.demo.controller;

import chem_data_platform.demo.service.ProjectService;
import chem_data_platform.demo.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Media file service controller
 * Provides access services for project files and images
 * Only project members or project owners are allowed to access
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    @Value("${upload.base-path:C:/uploads/chem_data_platform}")
    private String uploadBasePath;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private JwtUtil jwtUtil;    /**
     * Get project image
     * GET /media/projects/{projectId}/images/{fileName}
     * Requires a JWT token and project membership or a public project
     */    @GetMapping("/projects/{projectId}/images/{fileName:.+}")
    public ResponseEntity<?> getImage(
            @PathVariable Long projectId,
            @PathVariable String fileName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // TODO: Permission check - Only project members or project owners are allowed to access
            // if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            //             .body("Missing a valid authentication token");
            // }
            // 
            // String token = authHeader.substring(7);
            // if (!jwtUtil.isTokenValid(token)) {
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            //             .body("Invalid authentication token");
            // }
            // 
            // String username = jwtUtil.getUsernameFromToken(token);
            // 
            // boolean hasAccess = projectService.hasProjectAccessPermission(projectId, username);
            // if (!hasAccess) {
            //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
            //             .body("No permission to access media files in this project");
            // }
            
            // Build the full file path
            String filePath = uploadBasePath + File.separator + "projects" + File.separator + 
                            projectId + File.separator + "images" + File.separator + fileName;
            
            File file = new File(filePath);
            
            // Verify that the file exists
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // Validate path safety to prevent directory traversal attacks
            String canonicalPath = file.getCanonicalPath();
            String basePath = new File(uploadBasePath).getCanonicalPath();
            if (!canonicalPath.startsWith(basePath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Read file content
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            // Determine media type
            MediaType mediaType = getMediaType(fileName);
            
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(fileContent);
                    
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }    /**
     * Get project file
     * GET /media/projects/{projectId}/files/{fileName}
     * Requires a JWT token and project membership or a public project
     */    @GetMapping("/projects/{projectId}/files/{fileName:.+}")
    public ResponseEntity<?> getFile(
            @PathVariable Long projectId,
            @PathVariable String fileName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // TODO: Permission check - Only project members or project owners are allowed to access
            // if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            //             .body("Missing a valid authentication token");
            // }
            // 
            // String token = authHeader.substring(7);
            // if (!jwtUtil.isTokenValid(token)) {
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            //             .body("Invalid authentication token");
            // }
            // 
            // String username = jwtUtil.getUsernameFromToken(token);
            // 
            // boolean hasAccess = projectService.hasProjectAccessPermission(projectId, username);
            // if (!hasAccess) {
            //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
            //             .body("No permission to access media files in this project");
            // }
            
            // Build the full file path
            String filePath = uploadBasePath + File.separator + "projects" + File.separator + 
                            projectId + File.separator + "files" + File.separator + fileName;
            
            File file = new File(filePath);
            
            // Verify that the file exists
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // Validate path safety to prevent directory traversal attacks
            String canonicalPath = file.getCanonicalPath();
            String basePath = new File(uploadBasePath).getCanonicalPath();
            if (!canonicalPath.startsWith(basePath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Read file content
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            // Determine media type
            MediaType mediaType = getMediaType(fileName);
            
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(fileContent);
                    
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Infer media type from the file extension
     */
    private MediaType getMediaType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        switch (extension) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            case "webp":
                return MediaType.valueOf("image/webp");
            case "pdf":
                return MediaType.APPLICATION_PDF;
            case "txt":
                return MediaType.TEXT_PLAIN;
            case "csv":
                return MediaType.valueOf("text/csv");
            case "doc":
            case "docx":
                return MediaType.valueOf("application/msword");
            case "xls":
                return MediaType.valueOf("application/vnd.ms-excel");
            case "xlsx":
                return MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
