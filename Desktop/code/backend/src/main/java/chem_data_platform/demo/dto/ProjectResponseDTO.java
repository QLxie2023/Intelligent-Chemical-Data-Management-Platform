package chem_data_platform.demo.dto;

/**
 * 项目响应 DTO
 */
public class ProjectResponseDTO {

    private Long projectId;
    private String name;
    private String description;
    private String visibility;
    private String ownerUsername;

    // 构造器
    public ProjectResponseDTO() {
    }

    public ProjectResponseDTO(Long projectId, String name, String description, String visibility, String ownerUsername) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.ownerUsername = ownerUsername;
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
}
