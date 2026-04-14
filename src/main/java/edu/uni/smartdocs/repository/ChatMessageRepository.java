package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.ChatMessage;
import edu.uni.smartdocs.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Lấy lịch sử chat theo sessionId
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);

    // 🟢 Lấy DANH SÁCH SESSION (cuộc trò chuyện) của user
    @Query("SELECT DISTINCT c.sessionId, MIN(c.sessionName), MIN(c.timestamp) " +
            "FROM ChatMessage c WHERE c.user = :user " +
            "GROUP BY c.sessionId ORDER BY MIN(c.timestamp) DESC")
    List<Object[]> findDistinctSessionsByUser(@Param("user") User user);

    // Lấy lịch sử chat theo user
    List<ChatMessage> findByUserOrderByTimestampAsc(User user);

    // Lấy N tin nhắn gần nhất
    @Query("SELECT c FROM ChatMessage c WHERE c.user = :user ORDER BY c.timestamp DESC")
    List<ChatMessage> findTop10ByUserOrderByTimestampDesc(@Param("user") User user);


}