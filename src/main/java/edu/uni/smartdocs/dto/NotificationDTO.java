package edu.uni.smartdocs.dto;

import edu.uni.smartdocs.models.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class NotificationDTO {

    private Long id;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String url;

    public static NotificationDTO from(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .url(n.getUrl())
                .build();
    }
}
