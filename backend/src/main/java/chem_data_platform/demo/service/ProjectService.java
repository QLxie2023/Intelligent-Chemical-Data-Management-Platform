package chem_data_platform.demo.service;

import chem_data_platform.demo.dto.CreateProjectDTO;
import chem_data_platform.demo.dto.FileUploadResponseDTO;
import chem_data_platform.demo.dto.ImageUploadResponseDTO;
import chem_data_platform.demo.dto.ProjectResponseDTO;
import chem_data_platform.demo.dto.ProjectDetailDTO;
import chem_data_platform.demo.dto.FileInfoDTO;
import chem_data_platform.demo.dto.ImageInfoDTO;
import chem_data_platform.demo.dto.SearchResultDTO;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;

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
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper mapper = new ObjectMapper();
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
     * Initialize: validate and create upload directory
     */
    @PostConstruct
    public void init() {
        if (uploadBasePath == null || uploadBasePath.isEmpty()) {
            uploadBasePath = "C:/uploads/chem_data_platform";
            System.out.println("⚠ uploadBasePath is empty; using default value: " + uploadBasePath);
        }
        
        // Ensure the base directory exists
        File baseDir = new File(uploadBasePath);
        if (!baseDir.exists()) {
            boolean created = baseDir.mkdirs();
            if (created) {
                System.out.println("✓ Upload base directory created: " + baseDir.getAbsolutePath());
            } else {
                System.err.println("✗ Unable to create upload base directory: " + baseDir.getAbsolutePath());
            }
        } else {
            System.out.println("✓ Upload base directory already exists: " + baseDir.getAbsolutePath());
        }
    }

    public ProjectResponseDTO createProject(CreateProjectDTO dto, String username) {
        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        String vis = dto.getVisibility() == null ? "PRIVATE" : dto.getVisibility().toUpperCase();
        if (!vis.equals("PUBLIC")) {
            vis = "PRIVATE";
        }
        project.setVisibility(vis);
        project.setOwnerId(user.getId());
        project.setOwnerUsername(username);  // Set @Transient field for response
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
     * Get the current logged-in user's project list
     * Returns：
     * 1. Projects owned by the user, whether public or private
     * 2. All public projects created by other users
     */
    public List<ProjectResponseDTO> getUserProjects(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        
        // Get projects owned by the user
        List<Project> ownedProjects = projectRepository.findByOwnerId(user.getId());
        
        // Get all public projects
        List<Project> publicProjects = projectRepository.findByVisibility("PUBLIC");
        
        // Merge and deduplicate
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
     * Get projects owned by the user, private projects only
     */
    public List<ProjectResponseDTO> getUserPrivateProjects(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        
        List<Project> projects = projectRepository.findByOwnerId(user.getId());
        return projects.stream()
                .filter(p -> "PRIVATE".equals(p.getVisibility()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all public projects
     */
    public List<ProjectResponseDTO> getPublicProjects() {
        List<Project> projects = projectRepository.findByVisibility("PUBLIC");
        return projects.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check whether the project exists
     */
    public boolean projectExists(Long projectId) {
        return projectRepository.existsById(projectId);
    }

    /**
     * Check whether the user has permission to upload to this project
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
     * Check whether the user has permission to access project media files
     * Permission rules：
     * 1. Project owner can access
     * 2. Any authenticated user can access public projects
     * 3. Only the owner can access private projects
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
        
        // Project owner always has permission
        if (p.getOwnerId().equals(user.getId())) {
            return true;
        }
        
        // Public projects allow all authenticated users to access
        if ("PUBLIC".equals(p.getVisibility())) {
            return true;
        }
        
        // Private projects only allow the owner to access
        return false;
    }    /**
     * Upload file
     * Flow: 1. validate -> 2. save file to disk -> 3. save file metadata to database -> 4. set analysis status to pending
     */
    public FileUploadResponseDTO uploadFile(Long projectId, MultipartFile file, String username) throws IOException {
        if (!projectExists(projectId)) {
            throw new IllegalArgumentException("Project does not exist");
        }

        // Get userID
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        Long userId = user.getId();

        // Secondary validation: size and type
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds the limit");
        }
        java.util.Set<String> allowed = allowedFileTypes();
        if (!allowed.isEmpty() && (file.getContentType() == null || !allowed.contains(file.getContentType()))) {
            throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
        }

        // 1. Create upload directory and save file to disk
        File uploadDir = new File(uploadBasePath + "/projects/" + projectId + "/files/");
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("Unable to create upload directory: " + uploadDir.getAbsolutePath());
            }
            System.out.println("✓ Upload directory created: " + uploadDir.getAbsolutePath());
        }        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File targetFile = new File(uploadDir, fileName);
        file.transferTo(targetFile);
        System.out.println("✓ File saved to disk: " + targetFile.getAbsolutePath());

        // 2. Save file metadata to the database first as the original file record
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
        fileInfo.setAnalysisStatus("PENDING");  // Initial status: pending analysis

        FileInfo savedFileInfo = fileInfoRepository.save(fileInfo);
        System.out.println("✓ File saved to database，fileId: " + savedFileInfo.getId() + "，fileName: " + fileName);

        // 3. Build response DTO
        FileUploadResponseDTO dto = new FileUploadResponseDTO(
                savedFileInfo.getId(),
                savedFileInfo.getFileName(),
                savedFileInfo.getFileType(),
                savedFileInfo.getAccessUrl(),
                savedFileInfo.getUploadTimestamp().format(FORMATTER)
        );

        // File uploaded successfully. Analysis will be triggered by the frontend through POST /api/v1/files/{fileId}/analysis
        System.out.println("✓ File upload completed. Analysis can be triggered through POST /api/v1/files/" + savedFileInfo.getId() + "/analysis");

        return dto;
    }
    /**
     * Upload image
     * Flow: 1. validate -> 2. save image to disk -> 3. save image metadata to database -> 4. call AI API for analysis -> 5. save analysis result
     */
    public ImageUploadResponseDTO uploadImage(Long projectId, MultipartFile image, String username) throws IOException {
        System.out.println("=== Start uploading image ===");
        System.out.println("uploadBasePath: " + uploadBasePath);
        System.out.println("projectId: " + projectId);
        System.out.println("image.getOriginalFilename(): " + image.getOriginalFilename());
        System.out.println("image.getContentType(): " + image.getContentType());
        
        if (!projectExists(projectId)) {
            throw new IllegalArgumentException("Project does not exist");
        }

        // Get userID
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        Long userId = user.getId();

        // Secondary validation: size and image type
        if (image.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Image size exceeds the limit");
        }
        java.util.Set<String> allowedImg = allowedImageTypes();
        if (!allowedImg.isEmpty() && (image.getContentType() == null || !allowedImg.contains(image.getContentType()))) {
            throw new IllegalArgumentException("Unsupported image type: " + image.getContentType());
        }

        // 1. Create upload directory and save image to disk
        try {
            File uploadDir = new File(uploadBasePath + "/projects/" + projectId + "/images/");
            System.out.println("uploadDir path: " + uploadDir.getAbsolutePath());
            
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    throw new IOException("Unable to create upload directory: " + uploadDir.getAbsolutePath());
                }
                System.out.println("✓ Upload directory created: " + uploadDir.getAbsolutePath());
            } else {
                System.out.println("✓ Upload directory already exists: " + uploadDir.getAbsolutePath());
            }            String imageName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            File targetFile = new File(uploadDir, imageName);
            image.transferTo(targetFile);            System.out.println("✓ Image saved to disk: " + targetFile.getAbsolutePath());

            // Build image URL using a relative path for frontend proxy forwarding
            String imageUrl = "/media/projects/" + projectId + "/images/" + imageName;// 2. Save image metadata to the database first as the original image record
            ImageInfo imageInfo = new ImageInfo();
            imageInfo.setProjectId(projectId);
            imageInfo.setImageName(imageName);
            imageInfo.setImageUrl(imageUrl);
            imageInfo.setUploadTimestamp(LocalDateTime.now());
            imageInfo.setUploaderId(userId);

            ImageInfo savedImageInfo = imageInfoRepository.save(imageInfo);
            System.out.println("✓ Image saved to database，imageId: " + savedImageInfo.getImageId() + "，imageName: " + imageName);            // 3. Build response DTO
            ImageUploadResponseDTO dto = new ImageUploadResponseDTO(
                    savedImageInfo.getImageId(),
                    savedImageInfo.getImageName(),
                    savedImageInfo.getImageUrl(),
                    savedImageInfo.getUploadTimestamp().format(FORMATTER)
            );

            System.out.println("✓ Image upload completed, ready to trigger analysis");

            return dto;
        } catch (IOException e) {
            System.err.println("✗ Image upload failed (I/O exception): " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("✗ Image upload failed (other exception): " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Image upload failed: " + e.getMessage(), e);
        }
    }    
    /**
     * Convert Project entity to DTO, including ownerUsername
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
     * Get project details
     */
    public ProjectDetailDTO getProjectDetail(Long projectId, String username) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("Project does not exist");
        }

        Project project = projectOpt.get();
        
        // Permission check: project owner or public project
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        
        boolean isOwner = project.getOwnerId().equals(user.getId());
        boolean isPublic = "PUBLIC".equals(project.getVisibility());
        
        if (!isOwner && !isPublic) {
            throw new IllegalArgumentException("No permission to access this project");
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
     * Get project file list
     */
    public List<FileInfoDTO> getProjectFiles(Long projectId, String username) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("Project does not exist");
        }

        Project project = projectOpt.get();
        
        // Permission check
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        
        boolean isOwner = project.getOwnerId().equals(user.getId());
        boolean isPublic = "PUBLIC".equals(project.getVisibility());
        
        if (!isOwner && !isPublic) {
            throw new IllegalArgumentException("No permission to access this project");
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
     * Get project image list
     */
    public List<ImageInfoDTO> getProjectImages(Long projectId, String username) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("Project does not exist");
        }

        Project project = projectOpt.get();
        
        // Permission check
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        
        boolean isOwner = project.getOwnerId().equals(user.getId());
        boolean isPublic = "PUBLIC".equals(project.getVisibility());
        
        if (!isOwner && !isPublic) {
            throw new IllegalArgumentException("No permission to access this project");
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
     * Infer file type from MIME type
     * Return value: DOCUMENT/IMAGE/AUDIO/VIDEO/ARCHIVE/OTHER
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
     * Get file analysis status
     * Return Map includes: status, summary, tableData, errorReason, startTime
     */
    public Map<String, Object> getFileAnalysisStatus(Long fileId) {
        Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
        if (!fileOpt.isPresent()) {
            return null;
        }

        FileInfo file = fileOpt.get();
        Map<String, Object> result = new HashMap<>();
        
        String status = file.getAnalysisStatus();
        if (status == null) {
            status = "PENDING";
        }
        result.put("status", status);
        result.put("startTime", file.getAnalysisStartTime());

        if (file.getAnalysisData() != null && !file.getAnalysisData().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> analysisDataMap = mapper.readValue(
                    file.getAnalysisData(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
                result.put("summary", analysisDataMap.get("summary"));
                result.put("tableData", analysisDataMap.get("tableData"));
                result.put("keywords", analysisDataMap.get("keywords"));
                result.put("data_description", analysisDataMap.get("data_description"));
                result.put("standardized_name", analysisDataMap.get("standardized_name"));
                result.put("analysisData", file.getAnalysisData());
            } catch (Exception e) {
                result.put("summary", file.getAnalysisData());
                result.put("tableData", null);
            }
        } else {
            result.put("summary", null);
            result.put("tableData", null);
        }
        result.put("confirmedData", file.getConfirmedData());

        if ("FAILED".equals(status) && file.getAnalysisErrorReason() != null) {
            result.put("errorReason", file.getAnalysisErrorReason());
        }

        return result;
    }

    /**
     * Update file analysis status
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
     * Save file analysis result
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
            System.out.println("✅ [DB] Analysis result saved: fileId=" + fileId + ", data length=" + analysisData.length());
        } else {
            System.err.println("❌ [DB] File does not exist: fileId=" + fileId);
        }
    }

    /**
     * Save manually entered keywords for spreadsheet files (Excel/CSV).
     * Merges with existing keywords (deduplicated).
     */
    @Transactional
    public void saveFileManualKeywords(Long fileId, List<String> keywords) {
        Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            FileInfo file = fileOpt.get();
            try {
                // Merge with existing keywords
                java.util.Set<String> merged = new java.util.LinkedHashSet<>();
                if (file.getConfirmedData() != null && !file.getConfirmedData().isEmpty()) {
                    JsonNode existing = mapper.readTree(file.getConfirmedData());
                    if (existing.has("keywords") && existing.get("keywords").isArray()) {
                        existing.get("keywords").forEach(n -> merged.add(n.asText()));
                    }
                }
                merged.addAll(keywords);

                Map<String, Object> data = new HashMap<>();
                data.put("keywords", new java.util.ArrayList<>(merged));
                data.put("standardized_name", "");
                data.put("summary", "");
                data.put("data_description", "");
                data.put("tableData", List.of());

                file.setConfirmedData(mapper.writeValueAsString(data));
                file.setAnalysisStatus("COMPLETED");
                file.setAnalysisEndTime(LocalDateTime.now());
                fileInfoRepository.save(file);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save keywords", e);
            }
        } else {
            throw new IllegalArgumentException("File not found");
        }
    }

    /**
     * Save file analysis error
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
            System.out.println("✅ [DB] Error information saved: fileId=" + fileId + ", error=" + errorReason);
        } else {
            System.err.println("❌ [DB] File does not exist: fileId=" + fileId);
        }
    }

    @Transactional
    public void saveFileConfirmedData(Long fileId, String confirmedData) {
        Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            FileInfo file = fileOpt.get();
            file.setConfirmedData(confirmedData);
            fileInfoRepository.save(file);
            System.out.println("✅ [DB] Confirmed data saved: fileId=" + fileId);
        } else {
            throw new IllegalArgumentException("File does not exist: fileId=" + fileId);
        }
    }

    @Transactional
    public void saveImageConfirmedData(Long imageId, String confirmedData) {
        Optional<ImageInfo> imgOpt = imageInfoRepository.findById(imageId);
        if (imgOpt.isPresent()) {
            ImageInfo image = imgOpt.get();
            image.setConfirmedData(confirmedData);
            imageInfoRepository.save(image);
            System.out.println("✅ [DB] Image confirmed data saved: imageId=" + imageId);
        } else {
            throw new IllegalArgumentException("Image does not exist: imageId=" + imageId);
        }
    }

    public Map<String, Object> getImageAnalysisStatus(Long imageId) {
        Optional<ImageInfo> imgOpt = imageInfoRepository.findById(imageId);
        if (!imgOpt.isPresent()) {
            return null;
        }

        ImageInfo image = imgOpt.get();
        Map<String, Object> result = new HashMap<>();
        
        String status = image.getAnalysisStatus();
        if (status == null) {
            status = "PENDING";
        }
        result.put("status", status);
        result.put("startTime", image.getAnalysisStartTime());

        if (image.getAnalysisData() != null && !image.getAnalysisData().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> analysisDataMap = mapper.readValue(
                    image.getAnalysisData(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
                result.put("summary", analysisDataMap.get("summary"));
                result.put("tableData", analysisDataMap.get("tableData"));
                result.put("keywords", analysisDataMap.get("keywords"));
                result.put("data_description", analysisDataMap.get("data_description"));
                result.put("standardized_name", analysisDataMap.get("standardized_name"));
                result.put("analysisData", image.getAnalysisData());
            } catch (Exception e) {
                result.put("summary", image.getAnalysisData());
                result.put("tableData", null);
            }
        } else {
            result.put("summary", null);
            result.put("tableData", null);
        }
        result.put("confirmedData", image.getConfirmedData());

        if ("FAILED".equals(status) && image.getAnalysisErrorReason() != null) {
            result.put("errorReason", image.getAnalysisErrorReason());
        }

        return result;
    }

    public void updateImageAnalysisStatus(Long imageId, String status) {
        Optional<ImageInfo> imgOpt = imageInfoRepository.findById(imageId);
        if (imgOpt.isPresent()) {
            ImageInfo image = imgOpt.get();
            image.setAnalysisStatus(status);
            if ("PROCESSING".equals(status)) {
                image.setAnalysisStartTime(LocalDateTime.now());
            }
            imageInfoRepository.save(image);
        }
    }

    @Transactional
    public void saveImageAnalysisResult(Long imageId, String analysisData) {
        Optional<ImageInfo> imgOpt = imageInfoRepository.findById(imageId);
        if (imgOpt.isPresent()) {
            ImageInfo image = imgOpt.get();
            image.setAnalysisStatus("COMPLETED");
            image.setAnalysisData(analysisData);
            image.setAnalysisEndTime(LocalDateTime.now());
            imageInfoRepository.save(image);
            System.out.println("✅ [DB] Image analysis result saved: imageId=" + imageId + ", data length=" + analysisData.length());
        } else {
            System.err.println("❌ [DB] Image does not exist: imageId=" + imageId);
        }
    }

    @Transactional
    public void saveImageAnalysisError(Long imageId, String errorReason) {
        Optional<ImageInfo> imgOpt = imageInfoRepository.findById(imageId);
        if (imgOpt.isPresent()) {
            ImageInfo image = imgOpt.get();
            image.setAnalysisStatus("FAILED");
            image.setAnalysisErrorReason(errorReason);
            image.setAnalysisEndTime(LocalDateTime.now());
            imageInfoRepository.save(image);
            System.out.println("✅ [DB] Image error information saved: imageId=" + imageId + ", error=" + errorReason);
        } else {
            System.err.println("❌ [DB] Image does not exist: imageId=" + imageId);
        }
    }

    /**
     * Search files/images by keyword across PUBLIC projects + user's own PRIVATE projects.
     * Searches inside the analysis_data JSON TEXT column for keyword matches
     * in standardized_name, summary, data_description, and keywords fields.
     */
    public List<SearchResultDTO> searchByKeyword(String keyword, String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return new ArrayList<>();
        }
        Long userId = user.getId();
        String like = "%" + keyword.replace("'", "''") + "%";

        // Search file_infos (both analysis_data and confirmed_data)
        String fileSql = "SELECT f.file_id, f.file_name, f.project_id, " +
                "p.name AS project_name, p.visibility, u.username AS owner_username, " +
                "f.analysis_data, f.confirmed_data " +
                "FROM file_infos f " +
                "JOIN projects p ON f.project_id = p.id " +
                "JOIN users u ON p.owner_id = u.id " +
                "WHERE f.analysis_status = 'COMPLETED' " +
                "AND (p.visibility = 'PUBLIC' OR p.owner_id = ?) " +
                "AND (f.analysis_data LIKE ? OR f.confirmed_data LIKE ?)";

        // Search image_infos (both analysis_data and confirmed_data)
        String imageSql = "SELECT i.image_id, i.image_name, i.project_id, " +
                "p.name AS project_name, p.visibility, u.username AS owner_username, " +
                "i.analysis_data, i.confirmed_data " +
                "FROM image_infos i " +
                "JOIN projects p ON i.project_id = p.id " +
                "JOIN users u ON p.owner_id = u.id " +
                "WHERE i.analysis_status = 'COMPLETED' " +
                "AND (p.visibility = 'PUBLIC' OR p.owner_id = ?) " +
                "AND (i.analysis_data LIKE ? OR i.confirmed_data LIKE ?)";

        List<SearchResultDTO> results = new ArrayList<>();

        jdbcTemplate.query(fileSql, (rs) -> {
            try {
                String dataStr = rs.getString("analysis_data");
                if (dataStr == null || dataStr.isEmpty()) {
                    dataStr = rs.getString("confirmed_data");
                }
                if (dataStr == null || dataStr.isEmpty()) return;

                JsonNode root = mapper.readTree(dataStr);
                SearchResultDTO dto = new SearchResultDTO();
                dto.setProjectId(rs.getLong("project_id"));
                dto.setProjectName(rs.getString("project_name"));
                dto.setOwnerUsername(rs.getString("owner_username"));
                dto.setVisibility(rs.getString("visibility"));
                dto.setFileId(rs.getLong("file_id"));
                dto.setFileName(rs.getString("file_name"));
                dto.setFileType("file");
                dto.setStandardizedName(getJsonText(root, "standardized_name"));
                dto.setSummary(getJsonText(root, "summary"));
                if (root.has("keywords") && root.get("keywords").isArray()) {
                    List<String> kws = new ArrayList<>();
                    root.get("keywords").forEach(n -> kws.add(n.asText()));
                    dto.setKeywords(kws);
                } else {
                    dto.setKeywords(new ArrayList<>());
                }
                results.add(dto);
            } catch (Exception ignored) {}
        }, userId, like, like);

        jdbcTemplate.query(imageSql, (rs) -> {
            try {
                String dataStr = rs.getString("analysis_data");
                if (dataStr == null || dataStr.isEmpty()) {
                    dataStr = rs.getString("confirmed_data");
                }
                if (dataStr == null || dataStr.isEmpty()) return;

                JsonNode root = mapper.readTree(dataStr);
                SearchResultDTO dto = new SearchResultDTO();
                dto.setProjectId(rs.getLong("project_id"));
                dto.setProjectName(rs.getString("project_name"));
                dto.setOwnerUsername(rs.getString("owner_username"));
                dto.setVisibility(rs.getString("visibility"));
                dto.setImageId(rs.getLong("image_id"));
                dto.setFileName(rs.getString("image_name"));
                dto.setFileType("image");
                dto.setStandardizedName(getJsonText(root, "standardized_name"));
                dto.setSummary(getJsonText(root, "summary"));
                if (root.has("keywords") && root.get("keywords").isArray()) {
                    List<String> kws = new ArrayList<>();
                    root.get("keywords").forEach(n -> kws.add(n.asText()));
                    dto.setKeywords(kws);
                } else {
                    dto.setKeywords(new ArrayList<>());
                }
                results.add(dto);
            } catch (Exception ignored) {}
        }, userId, like, like);

        return results;
    }

    private String getJsonText(JsonNode root, String field) {
        return root.has(field) ? root.get(field).asText() : "";
    }

    /**
     * Delete file
     * Permission check: only the file uploader or project owner can delete
     */
    @Transactional
    public void deleteFile(Long fileId, String username) {
        // Get user information
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        Long userId = user.getId();

        // Find file information
        Optional<FileInfo> fileOpt = fileInfoRepository.findById(fileId);
        if (fileOpt.isEmpty()) {
            throw new IllegalArgumentException("File does not exist");
        }

        FileInfo fileInfo = fileOpt.get();
        Long projectId = fileInfo.getProjectId();

        // Permission check: file uploader or project owner
        boolean isUploader = fileInfo.getUploaderId().equals(userId);
        boolean isProjectOwner = false;
        
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent()) {
            isProjectOwner = projectOpt.get().getOwnerId().equals(userId);
        }

        if (!isUploader && !isProjectOwner) {
            throw new IllegalArgumentException("No permission to delete this file");
        }

        // Delete physical file
        String filePath = fileInfo.getFilePath();
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    System.out.println("✓ Physical file deleted: " + filePath);
                } else {
                    System.err.println("✗ Unable to delete physical file: " + filePath);
                }
            }
        }

        // Delete database record
        fileInfoRepository.delete(fileInfo);
        System.out.println("✓ Database file record deleted，fileId: " + fileId);
    }

    /**
     * Delete image
     * Permission check: only the image uploader or project owner can delete
     */
    @Transactional
    public void deleteImage(Long imageId, String username) {
        // Get user information
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        Long userId = user.getId();

        // Find image information
        Optional<ImageInfo> imageOpt = imageInfoRepository.findById(imageId);
        if (imageOpt.isEmpty()) {
            throw new IllegalArgumentException("Image does not exist");
        }

        ImageInfo imageInfo = imageOpt.get();
        Long projectId = imageInfo.getProjectId();

        // Permission check: image uploader or project owner
        boolean isUploader = imageInfo.getUploaderId().equals(userId);
        boolean isProjectOwner = false;
        
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent()) {
            isProjectOwner = projectOpt.get().getOwnerId().equals(userId);
        }

        if (!isUploader && !isProjectOwner) {
            throw new IllegalArgumentException("No permission to delete this image");
        }

        // Delete physical file
        String imagePath = uploadBasePath + "/projects/" + projectId + "/images/" + imageInfo.getImageName();
        File file = new File(imagePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("✓ Physical image deleted: " + imagePath);
            } else {
                System.err.println("✗ Unable to delete physical image: " + imagePath);
            }
        }

        // Delete database record
        imageInfoRepository.delete(imageInfo);
        System.out.println("✓ Database image record deleted，imageId: " + imageId);
    }

    /**
     * Delete project
     * Permission check: only the project owner can delete
     */
    @Transactional
    public void deleteProject(Long projectId, String username) {
        // Get user information
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        Long userId = user.getId();

        // Find project information
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new IllegalArgumentException("Project does not exist");
        }

        Project project = projectOpt.get();

        // Permission check: only the project owner can delete
        if (!project.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("No permission to delete this project");
        }

        // Delete all files related to the project
        List<FileInfo> files = fileInfoRepository.findAll().stream()
                .filter(f -> f.getProjectId().equals(projectId))
                .collect(Collectors.toList());

        for (FileInfo file : files) {
            // Delete physical file
            String filePath = file.getFilePath();
            if (filePath != null) {
                File physicalFile = new File(filePath);
                if (physicalFile.exists()) {
                    boolean deleted = physicalFile.delete();
                    if (deleted) {
                        System.out.println("✓ Physical file deleted: " + filePath);
                    } else {
                        System.err.println("✗ Unable to delete physical file: " + filePath);
                    }
                }
            }
            // Delete database record
            fileInfoRepository.delete(file);
        }

        // Delete all images related to the project
        List<ImageInfo> images = imageInfoRepository.findAll().stream()
                .filter(i -> i.getProjectId().equals(projectId))
                .collect(Collectors.toList());

        for (ImageInfo image : images) {
            // Delete physical file
            String imagePath = uploadBasePath + "/projects/" + projectId + "/images/" + image.getImageName();
            File physicalFile = new File(imagePath);
            if (physicalFile.exists()) {
                boolean deleted = physicalFile.delete();
                if (deleted) {
                    System.out.println("✓ Physical image deleted: " + imagePath);
                } else {
                    System.err.println("✗ Unable to delete physical image: " + imagePath);
                }
            }
            // Delete database record
            imageInfoRepository.delete(image);
        }

        // Delete project directory
        File projectDir = new File(uploadBasePath + "/projects/" + projectId);
        if (projectDir.exists()) {
            deleteDirectory(projectDir);
        }

        // Delete project record from database
        projectRepository.delete(project);
        System.out.println("✓ Project deleted，projectId: " + projectId);
    }

    /**
     * Recursively delete directory
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}