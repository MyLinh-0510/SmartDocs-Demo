package edu.uni.smartdocs.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "contact")
@Data
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // người gửi form
    private String name;
    private String email;
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String message;

    // admin trả lời
    @Column(length = 1000)
    private String adminReply;

    private boolean replied = false;

    private LocalDateTime createdAt;
    private LocalDateTime replyDate;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}