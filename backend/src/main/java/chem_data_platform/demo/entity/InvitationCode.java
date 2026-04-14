package chem_data_platform.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 邀请码实体
 */
@Entity
@Table(name = "invitation_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    // role 在数据库中为 ENUM，但在实体中使用 String 存储，移除 @Enumerated 注解
    @Column(name = "role", nullable = false, length = 50)
    private String role; // ROLE_ADMIN, ROLE_RESEARCHER

    @Column(name = "used", nullable = false)
    private Boolean used;

    @Column(name = "used_by")
    private Long usedById; // 使用者的 user id

    @Column(name = "used_at")
    private LocalDateTime usedAt; // 使用时间

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getIsUsed() {
        return used;
    }

    public void setIsUsed(Boolean isUsed) {
        this.used = isUsed;
    }

    public Long getUsedById() {
        return usedById;
    }

    public void setUsedById(Long usedById) {
        this.usedById = usedById;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
