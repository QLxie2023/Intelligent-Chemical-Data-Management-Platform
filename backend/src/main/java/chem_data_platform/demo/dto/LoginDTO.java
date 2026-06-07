package chem_data_platform.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "Username cannot be empty")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    // Manually add these two getters so IDEA stops reporting errors
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}