package edu.uni.smartdocs.service;

import edu.uni.smartdocs.dto.NotificationDTO;
import edu.uni.smartdocs.models.ContactMessage;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.Notification;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.NotificationRepository;
import edu.uni.smartdocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(User user, String message, String url) {

        if (user == null) return;

        Notification n = notificationRepository.save(
                Notification.builder()
                        .user(user)
                        .message(message)
                        .url(url)
                        .isRead(false)
                        .build()
        );

        // 🔥 REALTIME PUSH
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                NotificationDTO.from(n)
        );
    }

    public List<NotificationDTO> getMyNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationDTO::from)
                .toList();
    }

    public long countUnread(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long id, User user) {
        Notification n = notificationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new AccessDeniedException("Not your notification"));

        n.setIsRead(true);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    // === business notify ===

    public void notifyDocumentUploaded(Document doc) {

        String fileUrl = "/uploads/pdf/" + doc.getPdfFilename(); // 🔥 link chuẩn

        List<User> receivers = userRepository.findAll()
                .stream()
                .filter(User::isEnabled)
                .filter(u -> doc.getVisibleToRoles() != null
                        && doc.getVisibleToRoles().contains(u.getRole()))
                .filter(u -> !u.isAdmin())
                .toList();

        receivers.forEach(u ->
                create(
                        u,
                        "📄 Tài liệu mới: " + doc.getTitle(),
                        fileUrl // 🔥 truyền link
                )
        );
    }

    public void notifyContactReplied(ContactMessage message) {
        notifyAllRoles("✉️ Admin đã trả lời form liên hệ");
    }

    public void notifyAllRoles(String message) {
        userRepository.findByRoleIn(
                        List.of(User.Role.ADMIN, User.Role.EMPLOYEE, User.Role.CEO)
                ).stream()
                .filter(User::isEnabled) // 🔥 THÊM
                .forEach(u -> create(u, message, (String) null));    }
}
