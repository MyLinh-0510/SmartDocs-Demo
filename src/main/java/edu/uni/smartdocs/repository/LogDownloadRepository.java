package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.LogDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogDownloadRepository
        extends JpaRepository<LogDownload, Long> {

    @Query("""
        SELECT COUNT(l)
        FROM LogDownload l
        WHERE l.document.id = :docId
    """)
    long countByDocumentId(@Param("docId") Long docId);

    @Query("""
        SELECT COUNT(l)
        FROM LogDownload l
    """)
    long countAllDownloads();


    @Query("""
        SELECT ld.document.id, COUNT(ld.id)
        FROM LogDownload ld
        GROUP BY ld.document.id
        ORDER BY COUNT(ld.id) DESC
    """)
    List<Object[]> findTopDownloaded();

}


