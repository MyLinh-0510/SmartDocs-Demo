package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByIsVisibleTrue();
    List<Document> findByFileType_Id(Long fileTypeId);
    List<Document> findAll();
    Optional<Document> findById(Long id);
    boolean existsByMeta(String meta);


}