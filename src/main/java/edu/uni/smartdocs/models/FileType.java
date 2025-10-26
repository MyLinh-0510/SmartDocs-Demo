package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_types")
public class FileType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên loại file (PDF, DOCX, ...)
    @Column(nullable = false)
    private String name;

    // Phần mở rộng (.pdf, .docx, ...)
    private String extension;

    // Ngày tạo
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== CONSTRUCTORS =====
    public FileType() {
    }

    public FileType(String name, String extension) {
        this.name = name;
        this.extension = extension;
    }

    // ===== GETTERS & SETTERS =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

