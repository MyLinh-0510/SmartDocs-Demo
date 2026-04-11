package edu.uni.smartdocs.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemanticSearchResultDTO {
    private Long documentId;
    private String title;
    private String snippet;           // Đoạn văn bản ngắn liên quan
    private double similarityScore;   // Độ tương đồng (0.0 - 1.0)
}