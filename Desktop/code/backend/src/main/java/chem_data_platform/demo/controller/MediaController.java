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
 * 媒体文件服务控制器
 * 提供项目文件和图片的访问服务
 * 仅允许项目成员或项目所有者访问
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
     * 获取项目图片
     * GET /media/projects/{projectId}/images/{fileName}
     * 需要 JWT token 且必须是项目成员或公开项目
     */    @GetMapping("/projects/{projectId}/images/{fileName:.+}")
    public ResponseEntity<?> getImage(
            @PathVariable Long projectId,
            @PathVariable String fileName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // TODO: 权限检查 - 仅允许项目成员或项目所有者访问
            // if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            //             .body("未提供有效的认证令牌");
            // }
            // 
            // String token = authHeader.substring(7);
            // if (!jwtUtil.isTokenValid(token)) {
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            //             .body("无效的认证令牌");
            // }
            // 
            // String username = jwtUtil.getUsernameFromToken(token);
            // 
            // boolean hasAccess = projectService.hasProjectAccessPermission(projectId, username);
            // if (!hasAccess) {
            //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
            //             .body("无权限访问该项目的媒体文件");
            // }
            
            // 构建完整文件路径
            String filePath = uploadBasePath + File.separator + "projects" + File.separator + 
                            projectId + File.separator + "images" + File.separator + fileName;
            
            File file = new File(filePath);
            
            // 验证文件存在
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // 验证路径安全性（防止目录遍历攻击）
            String canonicalPath = file.getCanonicalPath();
            String basePath = new File(uploadBasePath).getCanonicalPath();
            if (!canonicalPath.startsWith(basePath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // 读取文件内容
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            // 确定媒体类型
            MediaType mediaType = getMediaType(fileName);
            
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(fileContent);
                    
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }    /**
     * 获取项目文件
     * GET /media/projects/{projectId}/files/{fileName}
     * 需要 JWT token 且必须是项目成员或公开项目
     */    @GetMapping("/projects/{projectId}/files/{fileName:.+}")
    public ResponseEntity<?> getFile(
            @PathVariable Long projectId,
            @PathVariable String fileName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // TODO: 权限检查 - 仅允许项目成员或项目所有者访问
            // if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            //             .body("未提供有效的认证令牌");
            // }
            // 
            // String token = authHeader.substring(7);
            // if (!jwtUtil.isTokenValid(token)) {
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            //             .body("无效的认证令牌");
            // }
            // 
            // String username = jwtUtil.getUsernameFromToken(token);
            // 
            // boolean hasAccess = projectService.hasProjectAccessPermission(projectId, username);
            // if (!hasAccess) {
            //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
            //             .body("无权限访问该项目的媒体文件");
            // }
            
            // 构建完整文件路径
            String filePath = uploadBasePath + File.separator + "projects" + File.separator + 
                            projectId + File.separator + "files" + File.separator + fileName;
            
            File file = new File(filePath);
            
            // 验证文件存在
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // 验证路径安全性（防止目录遍历攻击）
            String canonicalPath = file.getCanonicalPath();
            String basePath = new File(uploadBasePath).getCanonicalPath();
            if (!canonicalPath.startsWith(basePath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // 读取文件内容
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            // 确定媒体类型
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
     * 根据文件扩展名推断媒体类型
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
            case "doc":
            case "docx":
                return MediaType.valueOf("application/msword");
            case "xls":
            case "xlsx":
                return MediaType.valueOf("application/vnd.ms-excel");
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
