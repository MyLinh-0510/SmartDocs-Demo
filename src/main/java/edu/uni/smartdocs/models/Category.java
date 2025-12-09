package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên danh mục: Văn bản, Hợp đồng, Hóa đơn,...
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Mô tả chi tiết (tuỳ chọn)
    @Column(length = 255)
    private String description;

    // Nếu bạn muốn liên kết ngược với Document
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Document> documents;

    // --- Constructors ---
    public Category() {}

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

}
