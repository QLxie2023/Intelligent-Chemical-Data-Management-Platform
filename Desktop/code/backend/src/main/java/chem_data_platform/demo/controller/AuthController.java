package chem_data_platform.demo.controller;

import chem_data_platform.demo.dto.LoginDTO;
import chem_data_platform.demo.dto.LoginResponseDTO;
import chem_data_platform.demo.dto.RegisterDTO;
import chem_data_platform.demo.dto.UserInfoDTO;
import chem_data_platform.demo.entity.User;
import chem_data_platform.demo.service.UserService;
import chem_data_platform.demo.utils.JwtUtil;
import chem_data_platform.demo.vo.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    /**
     * 用户注册接口
     * 请求参数: username, password, invitationCode
     * 返回: ApiResponse<Void>
     * 状态码: 200 成功, 400 参数验证错误/邀请码无效, 409 用户名已存在
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterDTO dto) {
        try {
            // 验证参数
            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("用户名不能为空"));
            }
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("密码不能为空"));
            }
            if (dto.getInvitationCode() == null || dto.getInvitationCode().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("邀请码不能为空"));
            }

            // 检查用户名是否已存在
            if (userService.findByUsername(dto.getUsername()) != null) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ApiResponse.conflict("用户名已存在"));
            }

            // 创建用户并注册
            User user = new User();
            user.setUsername(dto.getUsername());
            user.setPassword(dto.getPassword());
            userService.register(user, dto.getInvitationCode());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("注册成功", null));        } catch (IllegalArgumentException e) {
            // 邀请码无效或已使用
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            // 其他异常
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("注册失败: " + e.getMessage()));
        }
    }

    /**
     * 用户登录接口
     * 请求参数: username, password
     * 返回: ApiResponse<LoginResponseDTO> 包含 token 和 user 信息
     * 状态码: 200 成功, 401 密码错误, 404 用户不存在
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginDTO dto) {
        try {
            // 验证参数
            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("用户名不能为空"));
            }
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("密码不能为空"));
            }

            // 检查用户是否存在
            User user = userService.findByUsername(dto.getUsername());
            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("用户不存在"));
            }

            // 进行身份验证
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );

            // 生成 token
            String token = jwtUtil.generateToken(dto.getUsername());

            // 构建用户信息 DTO
            UserInfoDTO userInfo = new UserInfoDTO(
                    user.getId().intValue(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getRole()
            );

            // 构建登录响应 DTO
            LoginResponseDTO loginResponse = new LoginResponseDTO(token, userInfo);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("登录成功", loginResponse));        } catch (BadCredentialsException e) {
            // 密码错误
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("密码错误"));
        } catch (RuntimeException e) {
            // 其他异常（如用户不存在的情况）
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("登录失败: " + e.getMessage()));
        }
    }
}
