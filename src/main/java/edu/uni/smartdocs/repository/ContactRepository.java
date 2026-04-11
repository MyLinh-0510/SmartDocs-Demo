package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    // ================= FORM CONTACT =================

    // lấy lịch sử theo email (user gửi form)
    List<Contact> findByEmailOrderByCreatedAtDesc(String email);

    // ================= ADMIN FILTER =================
    @Query("""
        SELECT c FROM Contact c
        WHERE (:replied IS NULL OR c.replied = :replied)
          AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
          AND (:toDate IS NULL OR c.createdAt <= :toDate)
    """)
    Page<Contact> filter(
            @Param("replied") Boolean replied,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}