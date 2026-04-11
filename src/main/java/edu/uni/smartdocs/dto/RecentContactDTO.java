package edu.uni.smartdocs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecentContactDTO {

    private Long contactId;
    private String otherEmail;
    private String otherName;
    private String avatar;
    private String lastMessage;
    private LocalDateTime lastTime;
    private int unreadCount;

    // Đổi tên field này (không bắt đầu bằng "is")
    private boolean withAdmin;     // ← Đổi từ isWithAdmin thành withAdmin
}