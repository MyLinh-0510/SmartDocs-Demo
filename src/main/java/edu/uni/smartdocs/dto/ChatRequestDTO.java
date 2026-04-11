package edu.uni.smartdocs.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequestDTO {
    private String message;           // Câu hỏi của người dùng
    private Long userId;
    private String sessionId;         // (tùy chọn) để nhóm cuộc trò chuyện
}