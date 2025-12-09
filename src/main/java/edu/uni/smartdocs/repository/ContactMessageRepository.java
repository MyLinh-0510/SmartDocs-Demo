package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.ContactMessage;
import edu.uni.smartdocs.models.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    // Lấy toàn bộ message theo Contact
    List<ContactMessage> findByContactOrderByCreatedAtAsc(Contact contact);

}
