package edu.uni.smartdocs.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tiêu đề tài liệu
    @Column(nullable = false)
    private String title;

    // Mô tả tài liệu
    @Column(columnDefinition = "TEXT")
    private String description;

    // Đường dẫn lưu file trên server
    private String filePath;

    // Tên file khi lưu trong server
    private String filename;

    // MIME type (vd: application/pdf)
    private String mimeType;

    // Kích thước file (bytes)
    private Long size;

    // Metadata bổ sung (nếu có)
    @Column(columnDefinition = "TEXT")
    private String meta;

    // Trạng thái hiển thị
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    // Thời điểm tạo
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Liên kết đến loại file
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "file_type_id")
    private FileType fileType;

    // Liên kết đến danh mục
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    // Vai trò được phép xem tài liệu này
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "document_visible_roles",
            joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "role")
    private Set<User.Role> visibleToRoles = new HashSet<>();

    @OneToMany(mappedBy = "document")
    private List<UserDocumentAction> actions;

    private String pdfFilename;

    private String pdfPath;


    // ===== CONSTRUCTOR =====
    public Document() {}

}
