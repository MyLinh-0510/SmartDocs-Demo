package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    // ================= CHAT =================

    @Query("""
        SELECT m FROM ContactMessage m
        WHERE (m.senderEmail = :me AND m.receiverEmail = :other)
           OR (m.senderEmail = :other AND m.receiverEmail = :me)
        ORDER BY m.createdAt ASC
    """)
    List<ContactMessage> findChat(String me, String other);

    // lấy tất cả tin của user (optional)
    List<ContactMessage> findBySenderEmailOrReceiverEmail(String sender, String receiver);
}