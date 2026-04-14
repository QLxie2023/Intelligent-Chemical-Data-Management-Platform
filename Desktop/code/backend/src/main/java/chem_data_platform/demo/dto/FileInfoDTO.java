package chem_data_platform.demo.dto;

import java.time.LocalDateTime;

/**
 * 文件信息 DTO
 */
public class FileInfoDTO {
    private Long fileId;
    private Long projectId;
    private String fileName;
    private String fileType;
    private LocalDateTime uploadTimestamp;
    private Long uploaderId;
    private String uploaderUsername;
    private String fileUrl;

    public FileInfoDTO() {
    }

    public FileInfoDTO(Long fileId, Long projectId, String fileName, String fileType, 
                       LocalDateTime uploadTimestamp, Long uploaderId, String uploaderUsername, String fileUrl) {
        this.fileId = fileId;
        this.projectId = projectId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.uploadTimestamp = uploadTimestamp;
        this.uploaderId = uploaderId;
        this.uploaderUsername = uploaderUsername;
        this.fileUrl = fileUrl;
    }

    // Getters and Setters
    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }    public String getUploaderUsername() {
        return uploaderUsername;
    }

    public void setUploaderUsername(String uploaderUsername) {
        this.uploaderUsername = uploaderUsername;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
