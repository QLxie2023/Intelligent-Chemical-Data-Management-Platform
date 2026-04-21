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

// 新增用于调试的导入
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
    private DataSource dataSource; // 注入 DataSource，便于打印连接信息

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    /**
     * 用邀请码注册用户
     */
    public User register(User user, String invitationCode) {
        // 规范化邀请码（去除前后空格，防止前端带空格导致查询失败）
        String code = invitationCode == null ? null : invitationCode.trim();

        // 调试日志：打印输入和数据源信息，帮助定位问题
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

        // 验证邀请码
        Optional<InvitationCode> invCodeOpt = invitationCodeRepository.findByCode(code);
        
        if (!invCodeOpt.isPresent()) {
            // 额外调试信息：列出表中前10条 code 帮助排查
            try {
                List<InvitationCode> all = invitationCodeRepository.findAll();
                logger.info("[INVITE-DEBUG] invitation_codes count={}, sampleCodes={}", all.size(), all.stream().limit(10).map(InvitationCode::getCode).toList());
            } catch (Exception e) {
                logger.warn("[INVITE-DEBUG] Failed to read invitation_codes table for debug", e);
            }

            logger.error("[INVITE-DEBUG] Invitation code '{}' not found in DB (trimmed='{}'). Throwing IllegalArgumentException.", invitationCode, code);
            throw new IllegalArgumentException("邀请码无效或已被使用，无法注册。");
        }
        
        InvitationCode invCode = invCodeOpt.get();
        if (Boolean.TRUE.equals(invCode.getIsUsed())) {
            logger.error("[INVITE-DEBUG] Invitation code '{}' has already been used (usedBy={}, usedAt={})", code, invCode.getUsedById(), invCode.getUsedAt());
            throw new IllegalArgumentException("邀请码无效或已被使用，无法注册。");
        }

        // 设置用户角色
        user.setRole(invCode.getRole());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // displayName 默认与 username 相同
        user.setDisplayName(user.getUsername());
        
        User savedUser = userRepository.save(user);
        
        // 标记邀请码为已使用
        invCode.setIsUsed(true);
        invCode.setUsedById(savedUser.getId());
        invCode.setUsedAt(LocalDateTime.now());
        invitationCodeRepository.save(invCode);
        
        return savedUser;
    }

    // 根据用户名查找用户
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    // 实现 UserDetailsService 接口，用于 Spring Security 认证
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
    }

    /**
     * 获取用户信息
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    /**
     * 更新用户信息
     */
    public void updateUser(User user) {
        userRepository.save(user);
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * 更新密码
     */
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}