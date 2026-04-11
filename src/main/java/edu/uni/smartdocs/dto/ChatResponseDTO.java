package edu.uni.smartdocs.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDTO {
    private String response;                    // Câu trả lời từ chatbot
    private List<RelatedDocumentDTO> sources;   // Các tài liệu tham chiếu
    private String timestamp;
}
