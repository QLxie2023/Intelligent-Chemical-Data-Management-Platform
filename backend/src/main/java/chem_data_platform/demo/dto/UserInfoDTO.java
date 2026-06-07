package chem_data_platform.demo.dto;

/**
 * User information response DTO
 */
public class UserInfoDTO {
    private Integer userId;
    private String username;
    private String email;
    private String displayName;
    private String role;

    // No-argument constructor
    public UserInfoDTO() {
    }

    // All-argument constructor
    public UserInfoDTO(Integer userId, String username, String email, String displayName, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
    }

    // Getter/Setter
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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
}
