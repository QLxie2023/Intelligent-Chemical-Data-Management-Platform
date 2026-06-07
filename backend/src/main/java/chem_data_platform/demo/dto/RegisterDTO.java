package chem_data_platform.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "Username cannot be empty")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    @NotBlank(message = "Invitation code cannot be empty")
    private String invitationCode;

    // Manually add getter
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getInvitationCode() { return invitationCode; }
}