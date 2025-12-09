package edu.uni.smartdocs.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "contact")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String userName;
    private String phone;
    private String userEmail;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String adminReply;
    private boolean replied = false;

    private LocalDateTime createdAt;
    private LocalDateTime replyDate;

    @OneToMany(mappedBy = "contact")
    @JsonManagedReference
    private List<ContactMessage> messages;


    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
