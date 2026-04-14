package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /* BASIC */

    List<Document> findByIsVisibleTrue();

    Optional<Document> findById(Long id);

    boolean existsByMeta(String meta);

    List<Document> findTop20ByIsVisibleTrueOrderByCreatedAtDesc();

    List<Document> findTop20ByOrderByCreatedAtDesc();

    Page<Document> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /*SEARCH (KHỚP JS)/user/search-api?keyword=&category=*/
    @Query("""
        SELECT DISTINCT d FROM Document d
        WHERE (:keyword IS NULL 
               OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR d.category.id = :categoryId)
          AND d.isVisible = true
          AND :role MEMBER OF d.visibleToRoles
    """)
    List<Document> searchDocuments(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("role") User.Role role
    );


    /* ACCESS AUTHOR (ROLE + FILTER)*/
    @Query("""
        SELECT DISTINCT d
        FROM Document d
        WHERE (:fileTypeId IS NULL OR d.fileType.id = :fileTypeId)
          AND (:categoryId IS NULL OR d.category.id = :categoryId)
          AND (:role IS NULL OR :role MEMBER OF d.visibleToRoles)
        ORDER BY d.createdAt DESC
    """)
    Page<Document> filterForAccessAuthor(
            @Param("fileTypeId") Long fileTypeId,
            @Param("categoryId") Long categoryId,
            @Param("role") User.Role role,
            Pageable pageable
    );

    /* 10 tài liệu mới nhất */
    @Query("""
    SELECT DISTINCT d FROM Document d
    WHERE d.isVisible = true
      AND :role MEMBER OF d.visibleToRoles
    ORDER BY d.createdAt DESC
""")
    List<Document> findTop10ForUser(@Param("role") User.Role role, Pageable pageable);


    /* CATEGORY */
    @Query("""
        SELECT d FROM Document d
        WHERE d.category.id = :categoryId
          AND d.isVisible = true
        ORDER BY d.createdAt DESC
    """)
    List<Document> findByCategory(@Param("categoryId") Long categoryId);

    /* CREATED / APPROVAL */

    List<Document> findByCreatedBy(User user);

    @Query("""
        SELECT d FROM Document d
        WHERE d.createdBy = :user
          AND d.category.name = 'Văn bản chưa duyệt'
    """)
    List<Document> findDocumentsForApprovalByUser(@Param("user") User user);

    List<Document> findByCreatedByAndStatus(User user, DocumentStatus status);

    List<Document> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    /* APPROVER*/

    List<Document> findByApproverAndStatusIn(
            User approver,
            List<DocumentStatus> statuses
    );

    List<Document> findByApproverOrderByCreatedAtDesc(User approver);

    List<Document> findByApproverAndStatusOrderByCreatedAtDesc(
            User approver,
            DocumentStatus status
    );

    List<Document> findByCreatedByAndStatusOrderByCreatedAtDesc(
            User createdBy,
            DocumentStatus status
    );

    /* STATISTIC */
    @Query("""
        SELECT COUNT(d)
        FROM Document d
        WHERE d.status = :status
          AND FUNCTION('YEAR', d.approvedAt) = :year
    """)
    long countByStatusAndYear(
            @Param("status") DocumentStatus status,
            @Param("year") int year
    );

    // Lọc
    @Query("""
        SELECT d FROM Document d
        WHERE d.deleted = false
          AND (:categoryId IS NULL OR d.category.id = :categoryId)
          AND (:fileTypeId IS NULL OR d.fileType.id = :fileTypeId)
          AND (:status IS NULL OR d.status = :status)
    """)
    Page<Document> filterForAdmin(
            Long categoryId,
            Long fileTypeId,
            DocumentStatus status,
            Pageable pageable
    );

    // Tìm tài liệu trình ký
    Page<Document> findAllByApproverIsNotNull(Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.approver IS NOT NULL AND (:status IS NULL OR d.status = :status)")
    Page<Document> findByApproverIsNotNullAndStatus(@Param("status") DocumentStatus status, Pageable pageable);

    @Query("""
    SELECT d FROM Document d
    WHERE d.deleted = false
      AND d.createdBy = :admin
      AND d.approver IS NULL
      AND (:categoryId IS NULL OR d.category.id = :categoryId)
      AND (:fileTypeId IS NULL OR d.fileType.id = :fileTypeId)
      AND (:status IS NULL OR d.status = :status)
    ORDER BY d.createdAt DESC
""")
    Page<Document> findAdminUploadedDocuments(
            @Param("admin") User admin,
            @Param("categoryId") Long categoryId,
            @Param("fileTypeId") Long fileTypeId,
            @Param("status") DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findByStatusAndApproverIsNotNull(
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findByStatusInAndApproverIsNotNull(
            List<DocumentStatus> statuses,
            Pageable pageable
    );

    @Query("""
    SELECT MAX(CAST(SUBSTRING(d.soVB, 1, 3) AS long))
    FROM Document d
    WHERE d.status = 'APPROVED'
    AND FUNCTION('YEAR', d.approvedAt) = :year
""")
    Long findMaxSoVBNumberByYear(int year);

    // Tìm kiếm theo title hoặc description (không phân biệt hoa thường)
    List<Document> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    // Tìm kiếm theo title
    List<Document> findByTitleContainingIgnoreCase(String title);

    // Tìm kiếm theo description
    List<Document> findByDescriptionContainingIgnoreCase(String description);

}
