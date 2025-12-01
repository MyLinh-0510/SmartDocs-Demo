package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "contact_messages")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;
    private String userEmail;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String message;

    private boolean replied = false;

    @Column(columnDefinition = "TEXT")
    private String adminReply;

    private LocalDateTime createdAt;
    private LocalDateTime replyDate;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
