package chem_data_platform.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "image_infos")
public class ImageInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "image_name", nullable = false, length = 255)
    private String imageName;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "upload_timestamp", nullable = true)
    private LocalDateTime uploadTimestamp = LocalDateTime.now();

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "analysis_status", nullable = true, length = 50)
    private String analysisStatus = "PENDING";

    @Column(name = "analysis_data", nullable = true, columnDefinition = "TEXT")
    private String analysisData;

    @Column(name = "confirmed_data", nullable = true, columnDefinition = "TEXT")
    private String confirmedData;

    @Column(name = "analysis_start_time", nullable = true)
    private LocalDateTime analysisStartTime;

    @Column(name = "analysis_end_time", nullable = true)
    private LocalDateTime analysisEndTime;

    @Column(name = "analysis_error_reason", nullable = true, length = 500)
    private String analysisErrorReason;

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getConfirmedData() {
        return confirmedData;
    }

    public void setConfirmedData(String confirmedData) {
        this.confirmedData = confirmedData;
    }
}
