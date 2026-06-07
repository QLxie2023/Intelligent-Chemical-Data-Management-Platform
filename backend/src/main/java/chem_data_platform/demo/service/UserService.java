package chem_data_platform.demo.service;

import chem_data_platform.demo.entity.InvitationCode;
import chem_data_platform.demo.entity.User;
import chem_data_platform.demo.repository.InvitationCodeRepository;
import chem_data_platform.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

// Import added for debugging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvitationCodeRepository invitationCodeRepository;

    @Autowired
    private DataSource dataSource; // Inject DataSource to print connection information

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    /**
     * Register user with an invitation code
     */
    public User register(User user, String invitationCode) {
        // Normalize the invitation code by trimming spaces to avoid frontend whitespace causing query failures
        String code = invitationCode == null ? null : invitationCode.trim();

        // Debug log: print input and data source information to help locate issues
        logger.info("[INVITE-DEBUG] incoming invitationCode='{}' trimmed='{}' for username='{}'", invitationCode, code, user.getUsername());
        try {
            if (dataSource != null) {
                try (Connection conn = dataSource.getConnection()) {
                    if (conn != null && conn.getMetaData() != null) {
                        logger.info("[INVITE-DEBUG] DataSource URL='{}' User='{}'", conn.getMetaData().getURL(), conn.getMetaData().getUserName());
                    }
                } catch (SQLException e) {
                    logger.warn("[INVITE-DEBUG] Failed to get connection from DataSource", e);
                }
            } else {
                logger.warn("[INVITE-DEBUG] dataSource is null");
            }
        } catch (Exception ex) {
            logger.warn("[INVITE-DEBUG] Unexpected error while logging DataSource info", ex);
        }

        // Validate invitation code
        Optional<InvitationCode> invCodeOpt = invitationCodeRepository.findByCode(code);
        
        if (!invCodeOpt.isPresent()) {
            // Extra debug information: list the first 10 codes in the table for troubleshooting
            try {
                List<InvitationCode> all = invitationCodeRepository.findAll();
                logger.info("[INVITE-DEBUG] invitation_codes count={}, sampleCodes={}", all.size(), all.stream().limit(10).map(InvitationCode::getCode).toList());
            } catch (Exception e) {
                logger.warn("[INVITE-DEBUG] Failed to read invitation_codes table for debug", e);
            }

            logger.error("[INVITE-DEBUG] Invitation code '{}' not found in DB (trimmed='{}'). Throwing IllegalArgumentException.", invitationCode, code);
            throw new IllegalArgumentException("Invitation code is invalid or has already been used, so registration is not allowed.");
        }
        
        InvitationCode invCode = invCodeOpt.get();
        if (Boolean.TRUE.equals(invCode.getIsUsed())) {
            logger.error("[INVITE-DEBUG] Invitation code '{}' has already been used (usedBy={}, usedAt={})", code, invCode.getUsedById(), invCode.getUsedAt());
            throw new IllegalArgumentException("Invitation code is invalid or has already been used, so registration is not allowed.");
        }

        // Set user role
        user.setRole(invCode.getRole());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // displayName defaults to username
        user.setDisplayName(user.getUsername());
        
        User savedUser = userRepository.save(user);
        
        // Mark invitation code as used
        invCode.setIsUsed(true);
        invCode.setUsedById(savedUser.getId());
        invCode.setUsedAt(LocalDateTime.now());
        invitationCodeRepository.save(invCode);
        
        return savedUser;
    }

    // Find user by username
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    // Implement UserDetailsService for Spring Security authentication
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User does not exist: " + username));
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
    }

    /**
     * Get user information
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    /**
     * Update user information
     */
    public void updateUser(User user) {
        userRepository.save(user);
    }

    /**
     * Validate password
     */
    public boolean verifyPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * Update password
     */
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}