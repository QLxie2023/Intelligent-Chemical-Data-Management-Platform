package chem_data_platform.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 图片信息实体
 */
@Entity
@Table(name = "image_infos")
public class ImageInfo {    @Id
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

    // Getters and Setters
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
    }    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }
}
