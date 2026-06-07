package chem_data_platform.demo.dto;

/**
 * Login response DTO
 */
public class LoginResponseDTO {
    private String token;
    private UserInfoDTO user;

    // No-argument constructor
    public LoginResponseDTO() {
    }

    // All-argument constructor
    public LoginResponseDTO(String token, UserInfoDTO user) {
        this.token = token;
        this.user = user;
    }

    // Getter/Setter
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfoDTO getUser() {
        return user;
    }

    public void setUser(UserInfoDTO user) {
        this.user = user;
    }
}
