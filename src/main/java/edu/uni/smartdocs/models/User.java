package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Data
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 10)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean isAdmin = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.EMPLOYEE;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    private String avatar;

    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<UserDocumentAction> actions;


    public enum Role {
        ADMIN, CEO, EMPLOYEE
    }

    public User() {
    }

    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        syncAdminFromRole();
    }

    // Đồng bộ isAdmin: chỉ ADMIN mới là admin (CEO không phải admin hệ thống)
    public void syncAdminFromRole() {
        this.isAdmin = (this.role == Role.ADMIN);
    }

    // === THÊM CÁC METHOD TIỆN LỢI ĐỂ KIỂM TRA ROLE ===
    public boolean isCEO() {
        return this.role == Role.CEO;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public boolean isEmployee() {
        return this.role == Role.EMPLOYEE;
    }

    public String getFullName() {
        return this.name != null && !this.name.trim().isEmpty()
                ? this.name
                : this.email;
    }

    // Optional: gọi sync khi set role (nếu dùng setter)
    public void setRole(Role role) {
        this.role = role;
        syncAdminFromRole();
    }
}