package edu.uni.smartdocs.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /* BASIC INFO */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /* FILE INFO */
    private String filePath;
    private String filename;
    private String mimeType;
    private Long size;

    @Column(columnDefinition = "TEXT")
    private String meta;

    /* PDF PREVIEW */
    private String pdfFilename;
    private String pdfPath;

    /* VISIBILITY */
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "document_visible_roles",
            joinColumns = @JoinColumn(name = "document_id")
    )
    @Column(name = "role")
    private Set<User.Role> visibleToRoles = new HashSet<>();

    /* CATEGORY & FILE TYPE */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "file_type_id")
    private FileType fileType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    /* TIME */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /* USER */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "so_vb")
    private String soVB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Column(columnDefinition = "TEXT")
    private String approvalNote;

    /* SOFT DELETE */
    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* USER ACTIONS */
    @OneToMany(mappedBy = "document")
    @JsonIgnore
    private List<UserDocumentAction> actions;

    /* VERSIONING */
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<DocumentVersion> versions;

    /* ==================== LIÊN KẾT AI ==================== */

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DocumentSummary documentSummary;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DocumentChunk> chunks;

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SemanticData semanticData;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AiRecommendation> aiRecommendations;

    /* ==================== CONSTRUCTOR & HELPER ==================== */
    public Document() {}

    // Helper methods
    public boolean hasSummary() {
        return documentSummary != null && documentSummary.getSummaryText() != null;
    }

    public boolean hasChunks() {
        return chunks != null && !chunks.isEmpty();
    }

    public boolean hasSemanticData() {
        return semanticData != null && semanticData.getEmbedding() != null;
    }

    public boolean hasAiRecommendations() {
        return aiRecommendations != null && !aiRecommendations.isEmpty();
    }
}