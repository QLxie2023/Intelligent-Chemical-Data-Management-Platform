package chem_data_platform.demo.dto;

import java.time.LocalDateTime;

/**
 * 项目详情 DTO
 */
public class ProjectDetailDTO {
    private Long projectId;
    private String name;
    private String description;
    private String visibility;
    private String ownerUsername;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProjectDetailDTO() {
    }

    public ProjectDetailDTO(Long projectId, String name, String description, String visibility, 
                           String ownerUsername, Long ownerId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.ownerUsername = ownerUsername;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
