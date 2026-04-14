package chem_data_platform.demo.dto;

import java.time.LocalDateTime;

/**
 * 图片信息 DTO
 */
public class ImageInfoDTO {
    private Long imageId;
    private Long projectId;
    private String imageName;
    private String imageUrl;
    private LocalDateTime uploadTimestamp;
    private Long uploaderId;
    private String uploaderUsername;

    public ImageInfoDTO() {
    }

    public ImageInfoDTO(Long imageId, Long projectId, String imageName, String imageUrl, 
                        LocalDateTime uploadTimestamp, Long uploaderId, String uploaderUsername) {
        this.imageId = imageId;
        this.projectId = projectId;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.uploadTimestamp = uploadTimestamp;
        this.uploaderId = uploaderId;
        this.uploaderUsername = uploaderUsername;
    }

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

    public String getUploaderUsername() {
        return uploaderUsername;
    }

    public void setUploaderUsername(String uploaderUsername) {
        this.uploaderUsername = uploaderUsername;
    }
}
