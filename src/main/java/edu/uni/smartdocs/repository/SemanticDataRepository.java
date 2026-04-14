package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.SemanticData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SemanticDataRepository extends JpaRepository<SemanticData, Long> {

    // Tìm kiếm theo documentId
    List<SemanticData> findByDocumentId(Long documentId);

    // Tìm kiếm ngữ nghĩa với PGvector (nếu database hỗ trợ)
    @Query(value = "SELECT *, 1 - (embedding::vector <=> cast(:embedding as vector)) as similarity " +
            "FROM semantic_data " +
            "WHERE 1 - (embedding::vector <=> cast(:embedding as vector)) > :threshold " +
            "ORDER BY similarity DESC LIMIT :limit",
            nativeQuery = true)
    List<SemanticData> findSimilarChunks(@Param("embedding") String embedding,
                                         @Param("threshold") double threshold,
                                         @Param("limit") int limit);
}