package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.Notification;
import edu.uni.smartdocs.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy tất cả thông báo của user
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Đếm số thông báo chưa đọc
    long countByUserAndIsReadFalse(User user);

    // Lấy top 5 (bell 🔔)
    List<Notification> findTop5ByUserOrderByCreatedAtDesc(User user);

    // Lấy thông báo chưa đọc
    List<Notification> findByUserAndIsReadFalse(User user);

    // Lấy notification theo id + user (bảo mật)
    Optional<Notification> findByIdAndUser(Long id, User user);

    // Phân trang
    Page<Notification> findByUser(User user, Pageable pageable);

    // Đánh dấu tất cả đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsRead(@Param("user") User user);
}
