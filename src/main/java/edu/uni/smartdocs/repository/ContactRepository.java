package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    // Lấy danh sách liên hệ theo email user
    List<Contact> findByUserEmailOrderByCreatedAtDesc(String userEmail);

}
