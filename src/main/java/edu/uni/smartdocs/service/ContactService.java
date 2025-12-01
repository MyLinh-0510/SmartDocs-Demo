package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.ContactMessage;
import edu.uni.smartdocs.repository.CategoryRepository;
import edu.uni.smartdocs.repository.ContactMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    private final ContactMessageRepository repo;

    public long count() {
        return repo.count();
    }

    public ContactService(ContactMessageRepository repo) {
        this.repo = repo;
    }

    public List<ContactMessage> getContactsByEmail(String email) {
        return repo.findByUserEmailOrderByCreatedAtDesc(email);
    }
}

