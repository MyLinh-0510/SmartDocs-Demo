package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import java.util.List;

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

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Document> getDocuments() { return documents; }
    public void setDocuments(List<Document> documents) { this.documents = documents; }
}
