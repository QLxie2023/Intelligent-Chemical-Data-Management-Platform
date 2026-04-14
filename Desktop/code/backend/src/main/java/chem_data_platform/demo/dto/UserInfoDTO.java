package chem_data_platform.demo.dto;

/**
 * 用户信息响应 DTO
 */
public class UserInfoDTO {
    private Integer userId;
    private String username;
    private String displayName;
    private String role;

    // 无参构造器
    public UserInfoDTO() {
    }

    // 全参构造器
    public UserInfoDTO(Integer userId, String username, String displayName, String role) {
        this.userId = userId;
        this.username = username;
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
