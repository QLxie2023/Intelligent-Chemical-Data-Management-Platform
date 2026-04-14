package chem_data_platform.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 文件信息实体
 */
@Entity
@Table(name = "file_infos")
public class FileInfo {    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;    
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;  // ENUM: DOCUMENT/IMAGE/AUDIO/VIDEO/ARCHIVE/OTHER

    @Column(name = "mime_type", nullable = true, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "upload_timestamp", nullable = true)
    private LocalDateTime uploadTimestamp;

    @Column(name = "access_url", nullable = true, length = 500)
    private String accessUrl;

    @Column(name = "is_deleted", nullable = true)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    // 分析相关字段
    @Column(name = "analysis_status", nullable = true, length = 50)
    private String analysisStatus = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "analysis_data", nullable = true, columnDefinition = "TEXT")
    private String analysisData; // JSON format: {summary, tableData: [...]}

    @Column(name = "analysis_start_time", nullable = true)
    private LocalDateTime analysisStartTime;

    @Column(name = "analysis_end_time", nullable = true)
    private LocalDateTime analysisEndTime;

    @Column(name = "analysis_error_reason", nullable = true, length = 500)
    private String analysisErrorReason;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }

    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public String getAnalysisData() {
        return analysisData;
    }

    public void setAnalysisData(String analysisData) {
        this.analysisData = analysisData;
    }

    public LocalDateTime getAnalysisStartTime() {
        return analysisStartTime;
    }

    public void setAnalysisStartTime(LocalDateTime analysisStartTime) {
        this.analysisStartTime = analysisStartTime;
    }

    public LocalDateTime getAnalysisEndTime() {
        return analysisEndTime;
    }

    public void setAnalysisEndTime(LocalDateTime analysisEndTime) {
        this.analysisEndTime = analysisEndTime;
    }

    public String getAnalysisErrorReason() {
        return analysisErrorReason;
    }

    public void setAnalysisErrorReason(String analysisErrorReason) {
        this.analysisErrorReason = analysisErrorReason;
    }
}
