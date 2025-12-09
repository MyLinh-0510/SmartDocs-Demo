package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.Contact;
import edu.uni.smartdocs.repository.ContactRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public long count() {
        return contactRepository.count();
    }

    public List<Contact> getContactsByEmail(String email) {
        return contactRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }
}
