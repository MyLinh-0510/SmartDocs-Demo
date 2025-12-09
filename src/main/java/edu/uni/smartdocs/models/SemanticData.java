package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "semantic_data")
public class SemanticData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(columnDefinition = "JSON")
    private String embedding;  // Lưu chuỗi JSON dạng mảng vector

    private LocalDateTime createdAt = LocalDateTime.now();
}
