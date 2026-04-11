package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.dto.NotificationDTO;
import edu.uni.smartdocs.security.CustomUserDetails;
import edu.uni.smartdocs.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;

    // Lấy thông báo cho header dropdown
    @GetMapping("/my")
    public List<NotificationDTO> myNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        System.out.println("USER = " + userDetails);

        if (userDetails == null) {
            return List.of();
        }

        return notificationService.getMyNotifications(userDetails.getUser());
    }

    // Đếm chưa đọc (badge)
    @GetMapping("/count")
    public Long countUnread(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return 0L;
        }
        return notificationService.countUnread(userDetails.getUser());
    }

    // Click thông báo → mark read
    @PostMapping("/{id}/read")
    public void markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) return;

        notificationService.markAsRead(id, userDetails.getUser());
    }
}
