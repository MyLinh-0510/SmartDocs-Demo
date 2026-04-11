package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.Contact;
import edu.uni.smartdocs.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    // ================= COUNT =================
    public long count() {
        return contactRepository.count();
    }

    // ================= HISTORY =================
    public List<Contact> getContactsByEmail(String email) {
        return contactRepository.findByEmailOrderByCreatedAtDesc(email);
    }

    // ================= FILTER =================
    public Page<Contact> filterContacts(
            Boolean replied,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {

        LocalDateTime from = (fromDate != null)
                ? fromDate.atStartOfDay()
                : null;

        LocalDateTime to = (toDate != null)
                ? toDate.atTime(23, 59, 59)
                : null;

        return contactRepository.filter(replied, from, to, pageable);
    }
}