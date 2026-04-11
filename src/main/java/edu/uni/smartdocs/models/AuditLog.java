package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------- Quan hệ --------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // -------- Log info --------
    @Column(name = "action", nullable = false)
    private String action; // UPLOAD, UPDATE, DELETE, DOWNLOAD...

    @Column(name = "target_type")
    private String targetType; // DOCUMENT, CONTACT, USER

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
