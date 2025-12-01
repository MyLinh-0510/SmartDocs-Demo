package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.ContactMessage;
import edu.uni.smartdocs.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findByUserEmailOrderByCreatedAtDesc(String userEmail);

}
