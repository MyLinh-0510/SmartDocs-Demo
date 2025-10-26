package edu.uni.smartdocs.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tiêu đề tài liệu
    @Column(nullable = false)
    private String title;

    // Mô tả
    @Column(columnDefinition = "TEXT")
    private String description;

    // Đường dẫn file trên server
    private String filePath;

    // Tên file trong server
    private String filename;

    // Tên gốc của file khi upload
    private String originalName;

    // Loại MIME (vd: application/pdf)
    private String mimeType;

    // Kích thước file (bytes)
    private Long size;

    // Metadata bổ sung (nếu có)
    @Column(columnDefinition = "TEXT")
    private String meta;

    // Hiển thị công khai hay riêng tư
    @Column(name = "is_visible")
    private Boolean isVisible = true;

    // Ngày tạo
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Liên kết đến loại file (bảng file_types)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_type_id")
    private FileType fileType;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;


    // ===== GETTER & SETTER =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public Boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Boolean visible) {
        isVisible = visible;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}