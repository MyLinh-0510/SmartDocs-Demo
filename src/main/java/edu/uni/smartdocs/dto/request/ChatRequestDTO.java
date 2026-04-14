package edu.uni.smartdocs.dto.request;

import lombok.Data;

@Data
public class ChatRequestDTO {
    private String message;
    private String sessionId;
    private Long userId;
    private double threshold = 0.7;
}