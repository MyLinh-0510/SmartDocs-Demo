package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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

    // CONSTRUCTORS
    public FileType() {
    }

    public FileType(String name, String extension) {
        this.name = name;
        this.extension = extension;
    }

}

