package edu.uni.smartdocs.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class SaveChatRequestDTO {
    private Long userId;
    private String sessionId;
    private String userMessage;
    private String botResponse;
    private List<Long> documentIds;
}