package chem_data_platform.demo.service;

import chem_data_platform.demo.dto.CreateProjectDTO;
import chem_data_platform.demo.dto.FileUploadResponseDTO;
import chem_data_platform.demo.dto.ImageUploadResponseDTO;
import chem_data_platform.demo.dto.ProjectResponseDTO;
import chem_data_platform.demo.dto.ProjectDetailDTO;
import chem_data_platform.demo.dto.FileInfoDTO;
import chem_data_platform.demo.dto.ImageInfoDTO;
import chem_data_platform.demo.entity.FileInfo;
import chem_data_platform.demo.entity.ImageInfo;
import chem_data_platform.demo.entity.Project;
import chem_data_platform.demo.entity.User;
import chem_data_platform.demo.repository.FileInfoRepository;
import chem_data_platform.demo.repository.ImageInfoRepository;
import chem_data_platform.demo.repository.ProjectRepository;
import chem_data_platform.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProjectService {
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Autowired
    private ImageInfoRepository imageInfoRepository;
      @Autowired
    private AnalysisService analysisService;@Value("${upload.base-path:C:/uploads/chem_data_platform}")
    private String uploadBasePath;

    @Value("${upload.max-file-size:10485760}")
    private long maxFileSize;

    @Value("${upload.allowed-file-types:}")
    private String allowedFileTypesRaw;    @Value("${upload.allowed-image-types:}")
    private String allowedImageTypesRaw;

    @Value("${app.server-url:http://localhost:8080}")
    private String serverUrl;

    private java.util.Set<String> allowedFileTypes() {
        if (allowedFileTypesRaw == null || allowedFileTypesRaw.trim().isEmpty()) return new java.util.HashSet<>();
        return new java.util.HashSet<>(java.util.Arrays.asList(allowedFileTypesRaw.split("\\s*,\\s*")));
    }

    private java.util.Set<String> allowedImageTypes() {
        if (allowedImageTypesRaw == null || allowedImageTypesRaw.trim().isEmpty()) return new java.util.HashSet<>();
        return new java.util.HashSet<>(java.util.Arrays.asList(allowedImageTypesRaw.split("\\s*,\\s*")));
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * 初始化：验证和创建上传目录
     */
    @PostConstruct
    public void init() {
        if (uploadBasePath == null || uploadBasePath.isEmpty()) {
            uploadBasePath = "C:/uploads/chem_data_platform";
            System.out.println("⚠ uploadBasePath 为空，使用默认值: " + uploadBasePath);
        }
        
        // 确保基础目录存在
        File baseDir = new File(uploadBasePath);
        if (!baseDir.exists()) {
            boolean created = baseDir.mkdirs();
            if (created) {
                System.out.println("✓ 上传基础目录已创建: " + baseDir.getAbsolutePath());
            } else {
                System.err.println("✗ 无法创建上传基础目录: " + baseDir.getAbsolutePath());
            }
        } else {
            System.out.println("✓ 上传基础目录已存在: " + baseDir.getAbsolutePath());
        }
    }

    public ProjectResponseDTO createProject(CreateProjectDTO dto, String username) {
        // 获取用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        String vis = dto.getVisibility() == null ? "PRIVATE" : dto.getVisibility().toUpperCase();
        if (!vis.equals("PUBLIC")) {
            vis = "PRIVATE";
        }
        project.setVisibility(vis);
        project.setOwnerId(user.getId());
        project.setOwnerUsername(username);  // 设置 @Transient 字段用于返回
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        Project savedProject = projectRepository.save(project);

        return new ProjectResponseDTO(
                savedProject.getProjectId(),
                savedProject.getName(),
                savedProject.getDescription(),
                savedProject.getVisibility(),
                savedProject.getOwnerUsername()
        );
    }

    /**
     * 获取当前登录用户的项目列表
     * 返回：
     * 1. 用户所有者的项目（无论公开或私有）
     * 2. 所有公开项目（由其他用户创建）
     */
    public List<ProjectResponseDTO> getUserProjects(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        // 获取用户拥有的项目
        List<Project> ownedProjects = projectRepository.findByOwnerId(user.getId());
        
        // 获取所有公开项目
        List<Project> publicProjects = projectRepository.findByVisibility("PUBLIC");
        
        // 合并并去重
        java.util.Set<Long> projectIds = new java.util.HashSet<>();
        List<Project> mergedProjects = new java.util.ArrayList<>();
        
        for (Project p : ownedProjects) {
            if (!projectIds.contains(p.getProjectId())) {
                projectIds.add(p.getProjectId());
                mergedProjects.add(p);
            }
        }
        
        for (Project p : publicProjects) {
            if (!projectIds.contains(p.getProjectId())) {
                projectIds.add(p.getProjectId());
                mergedProjects.add(p);
            }
        }
        
        return mergedProjects.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户所有者的项目（仅私有项目）
     */
    public List<ProjectResponseDTO> getUserPrivateProjects(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        List<Project> projects = projectRepository.findByOwnerId(user.getId());
        return projects.stream()
                .filter(p -> "PRIVATE".equals(p.getVisibility()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有公开项目
     */
    public List<ProjectResponseDTO> getPublicProjects() {
        List<Project> projects = projectRepository.findByVisibility("PUBLIC");
        return projects.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查项目是否存在
     */
    public boolean projectExists(Long projectId) {
        return projectRepository.existsById(projectId);
    }

    /**
     * 检查用户是否有权限上传到该项目
     */
    public boolean hasProjectPermission(Long projectId, String username) {
        User user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null) {
            return false;
        }
        
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            return false;
        }
        return project.get().getOwnerId().equals(user.getId());
    }    
    /**
     * 检查用户是否有权限访问项目媒体文件
     * 权限规则：
     * 1. 项目所有者可以访问
     * 2. 公开项目的任何已认证用户可以访问
     * 3. 私有项目只有所有者可以访问
     */
    public boolean hasProjectAccessPermission(Long projectId, String username) {
        User user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null) {
            return false;
        }
        
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            return false;
        }
        
        Project p = project.get();
        
        // 项目所有者总是有权限
        if (p.getOwnerId().equals(user.getId())) {
            return true;
        }
        
        // 公开项目允许所有已认证用户访问
        if ("PUBLIC".equals(p.getVisibility())) {
            return true;
        }
        
        // 私有项目只允许所有者访问
        return false;
    }    /**
     * 上传文件
     * 流程：1. 验证 → 2. 保存文件到磁盘 → 3. 保存文件信息到数据库 → 4. 更新分析状态为待处理
     */
    public FileUploadResponseDTO uploadFile(Long projectId, MultipartFile file, String username) throws IOException {
        if (!projectExists(projectId)) {
            throw new IllegalArgumentException("项目不存在");
        }

        // 获取用户ID
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Long userId = user.getId();

        // 二次校验：大小与类型
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超出限制");
        }
        java.util.Set<String> allowed = allowedFileTypes();
        if (!allowed.isEmpty() && (file.getContentType() == null || !allowed.contains(file.getContentType()))) {
            throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
        }

        // 1. 创建上传目录并保存文件到磁盘
        File uploadDir = new File(uploadBasePath + "/projects/" + projectId + "/files/");
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("无法创建上传目录: " + uploadDir.getAbsolutePath());
            }
            System.out.println("✓ 上传目录已创建: " + uploadDir.getAbsolutePath());
        }        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File targetFile = new File(uploadDir, fileName);
        file.transferTo(targetFile);
        System.out.println("✓ 文件已保存到磁盘: " + targetFile.getAbsolutePath());

        // 2. 先保存文件信息到数据库（原文件记录）
        FileInfo fileInfo = new FileInfo();
        fileInfo.setProjectId(projectId);
        fileInfo.setFileName(fileName);
        fileInfo.setFilePath(uploadDir.getAbsolutePath() + File.separator + fileName);
        fileInfo.setFileType(inferFileType(file.getContentType()));
        fileInfo.setMimeType(file.getContentType());
        fileInfo.setFileSize(file.getSize());        fileInfo.setUploaderId(userId);
        fileInfo.setUploadTimestamp(LocalDateTime.now());
        fileInfo.setAccessUrl("/media/projects/" + projectId + "/files/" + fileName);
        fileInfo.setIsDeleted(false);
        fileInfo.setAnalysisStatus("PENDING");  // 初始状态：待分析

        FileInfo savedFileInfo = fileInfoRepository.save(fileInfo);
        System.out.println("✓ 文件已保存到数据库，fileId: " + savedFileInfo.getId() + "，fileName: " + fileName);

        // 3. 构建响应 DTO
        FileUploadResponseDTO dto = new FileUploadResponseDTO(
                savedFileInfo.getId(),
                savedFileInfo.getFileName(),
                savedFileInfo.getFileType(),
                savedFileInfo.getUploadTimestamp().format(FORMATTER)
        );

        // 文件上传成功，分析将由前端发起请求 POST /api/v1/files/{fileId}/analysis 触发
        System.out.println("✓ 文件上传完成，可通过 POST /api/v1/files/" + savedFileInfo.getId() + "/analysis 触发 Kimi 分析");

        return dto;
    }
    /**
     * 上传图片
     * 流程：1. 验证 → 2. 保存图片到磁盘 → 3. 保存图片信息到数据库 → 4. 调用讯飞API进行分析 → 5. 保存分析结果
     */
    public ImageUploadResponseDTO uploadImage(Long projectId, MultipartFile image, String username) throws IOException {
        System.out.println("=== 开始上传图片 ===");
        System.out.println("uploadBasePath: " + uploadBasePath);
        System.out.println("projectId: " + projectId);
        System.out.println("image.getOriginalFilename(): " + image.getOriginalFilename());
        System.out.println("image.getContentType(): " + image.getContentType());
        
        if (!projectExists(projectId)) {
            throw new IllegalArgumentException("项目不存在");
        }

        // 获取用户ID
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Long userId = user.getId();

        // 二次校验：大小与图片类型
        if (image.getSize() > maxFileSize) {
            throw new IllegalArgumentException("图片大小超出限制");
        }
        java.util.Set<String> allowedImg = allowedImageTypes();
        if (!allowedImg.isEmpty() && (image.getContentType() == null || !allowedImg.contains(image.getContentType()))) {
            throw new IllegalArgumentException("不支持的图片类型: " + image.getContentType());
        }

        // 1. 创建上传目录并保存图片到磁盘
        try {
            File uploadDir = new File(uploadBasePath + "/projects/" + projectId + "/images/");
            System.out.println("uploadDir 路径: " + uploadDir.getAbsolutePath());
            
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    throw new IOException("无法创建上传目录: " + uploadDir.getAbsolutePath());
                }
                System.out.println("✓ 上传目录已创建: " + uploadDir.getAbsolutePath());
            } else {
                System.out.println("✓ 上传目录已存在: " + uploadDir.getAbsolutePath());
            }            String imageName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            File targetFile = new File(uploadDir, imageName);
            image.transferTo(targetFile);            System.out.println("✓ 图片已保存到磁盘: " + targetFile.getAbsolutePath());

            // 构建图片URL（使用相对路径，便于前端代理转发）
            String imageUrl = "/media/projects/" + projectId + "/images/" + imageName;// 2. 先保存图片信息到数据库（原图片记录）
            ImageInfo imageInfo = new ImageInfo();
            imageInfo.setProjectId(projectId);
            imageInfo.setImageName(imageName);
            imageInfo.setImageUrl(imageUrl);
            imageInfo.setUploadTimestamp(LocalDateTime.now());
            imageInfo.setUploaderId(userId);

            ImageInfo savedImageInfo = imageInfoRepository.save(imageInfo);
            System.out.println("✓ 图片已保存到数据库，imageId: " + savedImageInfo.getImageId() + "，imageName: " + imageName);            // 3. 构建响应 DTO
            ImageUploadResponseDTO dto = new ImageUploadResponseDTO(
                    savedImageInfo.getImageId(),
                    savedImageInfo.getImageName(),
                    savedImageInfo.getImageUrl(),
                    savedImageInfo.getUploadTimestamp().format(FORMATTER)
            );

            // 图片上传成功，分析不再自动触发
            System.out.println("✓ 图片上传完成");

            return dto;
        } catch (IOException e) {
            System.err.println("✗ 图片上传失败（IO异常）: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("✗ 图片上传失败（其他异常）: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("图片上传失败: " + e.getMessage(), e);
        }
    }    
    /**
     * 将 Project 实体转换为 DTO，包括获取 ownerUsername
     */
    private ProjectResponseDTO convertToDTO(Project project) {
        String ownerUsername = null;
        if (project.getOwnerId() != null) {
            User owner = userRepository.findById(project.getOwnerId()).orElse(null);
            ownerUsername = owner != null ? owner.getUsername() : "Unknown";
        }
        
        return new ProjectResponseDTO(
                project.getProjectId(),
                project.getName(),
                project.getDescription(),
                project.getVisibility(),
                ownerUsername
        );
    }

    /**
     * 获取项目详情
     */
    public ProjectDetailDTO getProjectDetail(Long projectId, String username) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("项目不存在");
        }

        Project project = projectOpt.get();
        
        // 权限检查：项目所有者或公开项目
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        boolean isOwner = project.getOwnerId().equals(user.getId());
        boolean isPublic = "PUBLIC".equals(project.getVisibility());
        
        if (!isOwner && !isPublic) {
            throw new IllegalArgumentException("无权限访问此项目");
        }

        User owner = userRepository.findById(project.getOwnerId()).orElse(null);
        String ownerUsername = owner != null ? owner.getUsername() : "Unknown";

        return new ProjectDetailDTO(
                project.getProjectId(),
                project.getName(),
                project.getDescription(),
                project.getVisibility(),
                ownerUsername,
                project.getOwnerId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    /**
     * 获取项目的文件列表
     */
    public List<FileInfoDTO> getProjectFiles(Long projectId, String username) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("项目不存在");
        }

        Project project = projectOpt.get();
        
        // 权限检查
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        boolean isOwner = project.getOwnerId().equals(user.getId());
        boolean isPublic = "PUBLIC".equals(project.getVisibility());
        
        if (!isOwner && !isPublic) {
            throw new IllegalArgumentException("无权限访问此项目");
        }        List<FileInfo> files = fileInfoRepository.findAll().stream()
                .filter(f -> f.getProjectId().equals(projectId))
                .collect(Collectors.toList());        return files.stream()
                .map(file -> {
                    String uploaderUsername = "Unknown";
                    if (file.getUploaderId() != null) {
                        User uploader = userRepository.findById(file.getUploaderId()).orElse(null);
                        uploaderUsername = uploader != null ? uploader.getUsername() : "Unknown";
                    }
                    return new FileInfoDTO(
                            file.getId(),
                            file.getProjectId(),
                            file.getFileName(),
                            file.getFileType(),
                            file.getUploadTimestamp(),
                            file.getUploaderId(),
                            uploaderUsername,
                            file.getAccessUrl()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取项目的图片列表
     */
    public List<ImageInfoDTO> getProjectImages(Long projectId, String username) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("项目不存在");
        }

        Project project = projectOpt.get();
        
        // 权限检查
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        boolean isOwner = project.getOwnerId().equals(user.getId());
        boolean isPublic = "PUBLIC".equals(project.getVisibility());
        
        if (!isOwner && !isPublic) {
            throw new IllegalArgumentException("无权限访问此项目");
        }

        List<ImageInfo> images = imageInfoRepository.findAll().stream()
                .filter(i -> i.getProjectId().equals(projectId))
                .collect(Collectors.toList());

        return images.stream()
                .map(image -> {
                    String uploaderUsername = "Unknown";
                    if (image.getUploaderId() != null) {
                        User uploader = userRepository.findById(image.getUploaderId()).orElse(null);
                        uploaderUsername = uploader != null ? uploader.getUsername() : "Unknown";
                    }
                    return new ImageInfoDTO(
                            image.getImageId(),
                            image.getProjectId(),
                            image.getImageName(),
                            image.getImageUrl(),
                            image.getUploadTimestamp(),
                            image.getUploaderId(),
                            uploaderUsername                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据 MIME 类型推断文件类型
     * 返回值: DOCUMENT/IMAGE/AUDIO/VIDEO/ARCHIVE/OTHER
     */
    private String inferFileType(String mimeType) {
        if (mimeType == null) {
            return "OTHER";
        }

        if (mimeType.startsWith("image/")) {
            return "IMAGE";
        } else if (mimeType.startsWith("audio/")) {
            return "AUDIO";
        } else if (mimeType.startsWith("video/")) {
            return "VIDEO";
        } else if (mimeType.contains("pdf") || mimeType.contains("word") || 
                   mimeType.contains("document") || mimeType.contains("text") ||
                   mimeType.contains("spreadsheet") || mimeType.contains("presentation")) {
            return "DOCUMENT";
        } else if (mimeType.contains("zip") || mimeType.contains("rar") || 
                   mimeType.contains("gzip") || mimeType.contains("7z") ||
                   mimeType.contains("archive") || mimeType.contains("compressed")) {
            return "ARCHIVE";        }
        return "OTHER";
    }    /**
     * 获取文件分析状态
     * 返回 Map 包含: status, summary, tableData, errorReason, startTime
     */
    public Map<String, Object> getFileAnalysisStatus(Long fileId) {
        Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
        if (!fileOpt.isPresent()) {
            return null;
        }

        FileInfo file = fileOpt.get();
        Map<String, Object> result = new HashMap<>();
        
        // 获取分析状态
        String status = file.getAnalysisStatus();
        if (status == null) {
            status = "PENDING";
        }
        result.put("status", status);
        result.put("startTime", file.getAnalysisStartTime());

        // 如果分析数据存在，尝试解析 JSON
        if (file.getAnalysisData() != null && !file.getAnalysisData().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> analysisDataMap = mapper.readValue(
                    file.getAnalysisData(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
                result.put("summary", analysisDataMap.get("summary"));
                result.put("tableData", analysisDataMap.get("tableData"));
            } catch (Exception e) {
                // JSON 解析失败，直接使用原始数据
                result.put("summary", file.getAnalysisData());
                result.put("tableData", null);
            }
        } else {
            result.put("summary", null);
            result.put("tableData", null);
        }

        // 如果分析失败，添加错误原因
        if ("FAILED".equals(status) && file.getAnalysisErrorReason() != null) {
            result.put("errorReason", file.getAnalysisErrorReason());
        }

        return result;
    }

    /**
     * 更新文件分析状态
     */
    public void updateFileAnalysisStatus(Long fileId, String status) {
        Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            FileInfo file = fileOpt.get();
            file.setAnalysisStatus(status);
            if ("PROCESSING".equals(status)) {
                file.setAnalysisStartTime(LocalDateTime.now());
            }
            fileInfoRepository.save(file);
        }
    }    /**
     * 保存文件分析结果
     */
    @Transactional
    public void saveFileAnalysisResult(Long fileId, String analysisData) {
        Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            FileInfo file = fileOpt.get();
            file.setAnalysisStatus("COMPLETED");
            file.setAnalysisData(analysisData);
            file.setAnalysisEndTime(LocalDateTime.now());
            fileInfoRepository.save(file);
            System.out.println("✅ [DB] 已保存分析结果: fileId=" + fileId + ", 数据长度=" + analysisData.length());
        } else {
            System.err.println("❌ [DB] 文件不存在: fileId=" + fileId);
        }
    }

    /**
     * 保存文件分析错误
     */
    @Transactional
    public void saveFileAnalysisError(Long fileId, String errorReason) {
        Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            FileInfo file = fileOpt.get();
            file.setAnalysisStatus("FAILED");
            file.setAnalysisErrorReason(errorReason);
            file.setAnalysisEndTime(LocalDateTime.now());
            fileInfoRepository.save(file);
            System.out.println("✅ [DB] 已保存错误信息: fileId=" + fileId + ", 错误=" + errorReason);
        } else {
            System.err.println("❌ [DB] 文件不存在: fileId=" + fileId);
        }
    }

    /**
     * 搜索项目
     */
    public List<ProjectResponseDTO> searchProjects(String query, String username) {
        // 实现搜索逻辑，或者暂时返回用户所有项目
        return getUserProjects(username);
    }
}
