package edu.uni.smartdocs.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResultDTO {
    private Long documentId;
    private String documentTitle;
    private String content;
    private String chunkText;
    private double score;
    private String source;
    private String metadata;

    // Constructor chính
    public SemanticSearchResultDTO(Long documentId, String documentTitle,
                                   String chunkText, double score) {
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.chunkText = chunkText;
        this.score = score;
    }

    // ===== THÊM CÁC METHOD NÀY ĐỂ TƯƠNG THÍCH =====

    // Getter cho title (để code gọi getTitle() vẫn chạy)
    public String getTitle() {
        return this.documentTitle;
    }

    // Setter cho title
    public void setTitle(String title) {
        this.documentTitle = title;
    }

    // Getter cho snippet (để code gọi getSnippet() vẫn chạy)
    public String getSnippet() {
        return this.chunkText != null ? this.chunkText : this.content;
    }

    // Setter cho snippet
    public void setSnippet(String snippet) {
        this.chunkText = snippet;
    }

    // Getter cho similarityScore (nếu code gọi getSimilarityScore)
    public double getSimilarityScore() {
        return this.score;
    }

    // Setter cho similarityScore
    public void setSimilarityScore(double similarityScore) {
        this.score = similarityScore;
    }
}