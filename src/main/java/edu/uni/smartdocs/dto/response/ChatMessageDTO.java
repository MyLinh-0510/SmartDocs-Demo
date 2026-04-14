package edu.uni.smartdocs.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageDTO {
    private Long id;
    private String content;           // Nội dung tin nhắn
    private String role;              // "USER" hoặc "BOT"
    private LocalDateTime timestamp;
    private String sessionId;
    private List<Long> documentIds;

    // Thêm field để phân biệt ai gửi
    private boolean fromUser;
    private String userMessage;
    private String botResponse;
}