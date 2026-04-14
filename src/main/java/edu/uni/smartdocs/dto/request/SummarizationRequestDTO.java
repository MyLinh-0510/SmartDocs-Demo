package edu.uni.smartdocs.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummarizationRequestDTO {
    private Long documentId;
    private Integer maxLength;        // Độ dài tóm tắt tối đa (tùy chọn)
}