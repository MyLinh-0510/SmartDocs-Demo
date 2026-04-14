package edu.uni.smartdocs.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
    private String referencedDocumentsInfo;     // chi tiết hơn

    @Column(name = "session_id", length = 100)
    private String sessionId;

    // 🟢 THÊM TRƯỜNG NÀY - ĐẶT TÊN CHO CUỘC TRÒ CHUYỆN
    @Column(name = "session_name", length = 255)
    private String sessionName;

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

    // Helper: Lấy danh sách document IDs từ JSON
    public List<Long> getReferencedDocumentIdList() {
        if (referencedDocumentIds == null || referencedDocumentIds.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(referencedDocumentIds, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Helper: Lưu danh sách document IDs thành JSON
    public void setReferencedDocumentIdList(List<Long> documentIds) {
        try {
            this.referencedDocumentIds = objectMapper.writeValueAsString(documentIds);
        } catch (Exception e) {
            e.printStackTrace();
            this.referencedDocumentIds = "[]";
        }
    }
}