package chem_data_platform.demo.controller;

import chem_data_platform.demo.dto.UserDetailDTO;
import chem_data_platform.demo.dto.UserInfoDTO;
import chem_data_platform.demo.entity.User;
import chem_data_platform.demo.repository.FileInfoRepository;
import chem_data_platform.demo.repository.ImageInfoRepository;
import chem_data_platform.demo.repository.ProjectRepository;
import chem_data_platform.demo.repository.UserRepository;
import chem_data_platform.demo.vo.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final FileInfoRepository fileInfoRepository;
    private final ImageInfoRepository imageInfoRepository;

    public UserController(UserRepository userRepository,
                         ProjectRepository projectRepository,
                         FileInfoRepository fileInfoRepository,
                         ImageInfoRepository imageInfoRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.fileInfoRepository = fileInfoRepository;
        this.imageInfoRepository = imageInfoRepository;
    }

    /**
     * Get all users list
     * Accessible to all authenticated users
     * Returns: ApiResponse<List<UserInfoDTO>> user list
     * Status: 200 success
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<UserInfoDTO>>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            List<UserInfoDTO> userInfoList = users.stream()
                    .map(user -> new UserInfoDTO(
                            user.getId().intValue(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getDisplayName(),
                            user.getRole()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Successfully fetched user list", userInfoList));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.serverError("Failed to fetch user list: " + e.getMessage()));
        }
    }

    /**
     * Delete user
     * Accessible only to admin (ROLE_ADMIN)
     * Request param: userId (path variable)
     * Returns: ApiResponse<Void>
     * Status: 200 success, 403 forbidden, 404 user not found
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        try {
            // Check if user exists
            if (!userRepository.existsById(userId)) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.notFound("User not found"));
            }

            // Check if trying to delete self
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth.getName();
            User userToDelete = userRepository.findById(userId).get();
            
            if (userToDelete.getUsername().equals(currentUsername)) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.badRequest("Cannot delete your own account"));
            }

            // Delete user
            userRepository.deleteById(userId);
            
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.serverError("Failed to delete user: " + e.getMessage()));
        }
    }

    /**
     * Get current logged-in user info
     * Returns current user's role and other info for frontend permission check
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<UserInfoDTO>> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            UserInfoDTO userInfo = new UserInfoDTO(
                    user.getId().intValue(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole()
            );
            
            return ResponseEntity.ok(ApiResponse.success("Successfully fetched current user", userInfo));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.serverError("Failed to fetch current user: " + e.getMessage()));
        }
    }

    /**
     * Get user detail with statistics
     * Returns user info including project count, file count and image count
     * Request param: userId (path variable)
     * Returns: ApiResponse<UserDetailDTO>
     * Status: 200 success, 404 user not found
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDetailDTO>> getUserDetail(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Query statistics from database
            long projectCount = projectRepository.findByOwnerId(userId).size();
            long fileCount = fileInfoRepository.countByUploaderId(userId);
            long imageCount = imageInfoRepository.countByUploaderId(userId);
            
            UserDetailDTO userDetail = new UserDetailDTO(
                    user.getId().intValue(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole().replace("ROLE_", ""), // Remove ROLE_ prefix
                    (int) projectCount,
                    (int) fileCount,
                    (int) imageCount
            );
            
            return ResponseEntity.ok(ApiResponse.success("Successfully fetched user detail", userDetail));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.serverError("Failed to fetch user detail: " + e.getMessage()));
        }
    }
}