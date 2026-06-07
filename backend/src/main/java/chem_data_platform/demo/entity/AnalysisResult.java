package chem_data_platform.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Analysis result entity（minimum viable version）
 */
@Entity
@Table(name = "analysis_results")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisId;

    // Associated project
    @Column(nullable = false)
    private Long projectId;

    // Optionally associated with either a file or an image
    private Long fileId;
    private Long imageId;

    // Brief summary（used for quick display）
    @Column(nullable = false, length = 1000)
    private String summary;

    // Raw response or detailed data (TEXT)
    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters / Setters
    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
