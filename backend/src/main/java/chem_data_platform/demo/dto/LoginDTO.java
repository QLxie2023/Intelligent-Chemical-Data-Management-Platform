package chem_data_platform.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    // 手动加上这两个 getter，IDEA 立刻不报错了！
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}