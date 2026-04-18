package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.DocumentSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DocumentSummaryRepository extends JpaRepository<DocumentSummary, Long> {
    Optional<DocumentSummary> findByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}