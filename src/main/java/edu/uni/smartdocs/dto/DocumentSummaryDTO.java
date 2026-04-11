package edu.uni.smartdocs.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSummaryDTO {
    private Long documentId;
    private String title;
    private String summaryText;
    private Integer originalLength;
    private Integer summaryLength;
    private String createdAt;
}
