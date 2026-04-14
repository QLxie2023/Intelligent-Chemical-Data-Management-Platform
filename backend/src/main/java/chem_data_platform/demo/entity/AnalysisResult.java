package chem_data_platform.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 分析结果实体（最小可用版本）
 */
@Entity
@Table(name = "analysis_results")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisId;

    // 关联项目
    @Column(nullable = false)
    private Long projectId;

    // 可选关联文件或图片中的一个
    private Long fileId;
    private Long imageId;

    // 简要摘要（用于快速展示）
    @Column(nullable = false, length = 1000)
    private String summary;

    // 原始返回或详细数据（TEXT）
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
