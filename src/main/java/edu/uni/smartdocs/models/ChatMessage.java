package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userMessage;

    @Column(columnDefinition = "TEXT")
    private String botResponse;

    @Column(columnDefinition = "JSON")
    private String referencedDocumentIds;       // "[1,5,12]"

    @Column(columnDefinition = "JSON")
    private String referencedDocumentsInfo;     // chi tiết hơn (tùy chọn)

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType = MessageType.USER;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Helper method
    public List<Long> getReferencedDocumentIdList() {
        if (referencedDocumentIds == null || referencedDocumentIds.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // TODO: Sử dụng ObjectMapper để parse JSON thành List<Long>
        return new ArrayList<>();
    }
}