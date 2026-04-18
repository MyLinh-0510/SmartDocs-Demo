package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);

    @Query(value = "SELECT * FROM document_chunks ORDER BY id LIMIT :limit", nativeQuery = true)
    List<DocumentChunk> findTopN(int limit);
}
