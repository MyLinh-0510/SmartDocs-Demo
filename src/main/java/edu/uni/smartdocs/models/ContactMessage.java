package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contact_message")
@Data   // ← Lombok sẽ tự sinh tất cả getter/setter
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderEmail;
    private String receiverEmail;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    // Giữ lại cho tương thích cũ (1 file)
    private String fileUrl;
    private String fileName;

    // Hỗ trợ nhiều ảnh / file
    @ElementCollection
    @CollectionTable(name = "contact_message_file_urls",
            joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "file_url")
    private List<String> fileUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "contact_message_file_names",
            joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "file_name")
    private List<String> fileNames = new ArrayList<>();

    // === THÊM FIELD NÀY ĐỂ HỖ TRỢ TẢI FILE GỐC ===
    @ElementCollection
    @CollectionTable(name = "contact_message_download_urls",
            joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "download_url")
    private List<String> downloadUrls = new ArrayList<>();

    private LocalDateTime createdAt;

    private Boolean isRead = false;

    @Column(name = "edited")
    private Boolean edited = false;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}