package chem_data_platform.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "邀请码不能为空")
    private String invitationCode;

    // 手动补上 getter
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getInvitationCode() { return invitationCode; }
}