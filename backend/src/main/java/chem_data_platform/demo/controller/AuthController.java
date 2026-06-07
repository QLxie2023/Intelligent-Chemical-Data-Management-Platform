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
import java.util.Map;

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
     * User registration API
     * Request parameters: username, password, invitationCode
     * Returns: ApiResponse<Void>
     * Status codes: 200 success, 400 parameter validation error/invalid invitation code, 409 Username already exists
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterDTO dto) {
        try {
            // Validate parameters
            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("Username cannot be empty"));
            }
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("Password cannot be empty"));
            }
            if (dto.getInvitationCode() == null || dto.getInvitationCode().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("Invitation code cannot be empty"));
            }

            // Check whether the username already exists
            if (userService.findByUsername(dto.getUsername()) != null) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ApiResponse.conflict("Username already exists"));
            }

            // Create the user and register
            User user = new User();
            user.setUsername(dto.getUsername());
            user.setPassword(dto.getPassword());
            userService.register(user, dto.getInvitationCode());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Registration successful", null));        } catch (IllegalArgumentException e) {
            // Invitation code is invalid or has already been used
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            // Other exceptions
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Registration failed: " + e.getMessage()));
        }
    }

    /**
     * User login API
     * Request parameters: username, password
     * Returns: ApiResponse<LoginResponseDTO> Includes token and user information
     * Status codes: 200 success, 401 Incorrect password, 404 User does not exist
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginDTO dto) {
        try {
            // Validate parameters
            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("Username cannot be empty"));
            }
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("Password cannot be empty"));
            }

            // Check whether the user exists
            User user = userService.findByUsername(dto.getUsername());
            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User does not exist"));
            }

            // Authenticate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );

            // Generate token
            String token = jwtUtil.generateToken(dto.getUsername());

            // Build user information DTO
            UserInfoDTO userInfo = new UserInfoDTO(
                    user.getId().intValue(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole()
            );

            // Build login response DTO
            LoginResponseDTO loginResponse = new LoginResponseDTO(token, userInfo);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Login successful", loginResponse));
        } catch (BadCredentialsException e) {
            // Incorrect password
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("Incorrect password"));
        } catch (RuntimeException e) {
            // Other exceptions, such as when the user does not exist
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("Login failed: " + e.getMessage()));
        }
    }

    /**
     * Get user information API
     * Request parameters: username (query param)
     * Returns: ApiResponse<UserInfoDTO> user information
     * Status codes: 200 success, 404 User does not exist
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoDTO>> getCurrentUser(@RequestParam String username) {
        try {
            // Find user
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User does not exist"));
            }

            // Build user information DTO
            UserInfoDTO userInfo = new UserInfoDTO(
                    user.getId().intValue(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole()
            );

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("User information retrieved successfully", userInfo));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Failed to retrieve user information: " + e.getMessage()));
        }
    }

    /**
     * Change password API
     * Request parameters: username (query param), oldPassword, newPassword (body)
     * Returns: ApiResponse<Void>
     * Status codes: 200 success, 400 parameter error, 401 Old password is incorrect, 404 User does not exist
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestParam String username, @RequestBody Map<String, String> passwordData) {
        try {
            // Validate parameters
            if (!passwordData.containsKey("oldPassword") || !passwordData.containsKey("newPassword")) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.badRequest("Please provide the old password and new password"));
            }

            // Find user
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User does not exist"));
            }

            // Validate old password
            boolean isPasswordValid = userService.verifyPassword(user, passwordData.get("oldPassword"));
            if (!isPasswordValid) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Old password is incorrect"));
            }

            // Update password
            userService.updatePassword(user, passwordData.get("newPassword"));

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("Password changed successfully", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("Failed to change password: " + e.getMessage()));
        }
    }
}
