package chem_data_platform.demo.dto;

/**
 * 用户详情DTO，包含统计信息
 */
public class UserDetailDTO {
    private Integer id;
    private String username;
    private String email;
    private String displayName;
    private String role;
    private Integer projectCount;
    private Integer fileCount;
    private Integer imageCount;

    public UserDetailDTO() {
    }

    public UserDetailDTO(Integer id, String username, String email, String displayName, String role,
                         Integer projectCount, Integer fileCount, Integer imageCount) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.projectCount = projectCount;
        this.fileCount = fileCount;
        this.imageCount = imageCount;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(Integer projectCount) {
        this.projectCount = projectCount;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public Integer getImageCount() {
        return imageCount;
    }

    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }
}