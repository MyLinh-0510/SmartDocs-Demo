package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByPhone(String phone);

    List<User> findByRole(User.Role role);


    Optional<User> findByResetToken(String token);

    long countByIsAdminTrue();

    @Query("SELECT MONTH(u.createdAt), COUNT(u) FROM User u " +
            "WHERE YEAR(u.createdAt) = :year " +
            "GROUP BY MONTH(u.createdAt)")
    List<Object[]> countUsersByMonth(@Param("year") int year);

    List<User> findByRoleIn(List<User.Role> roles);

    long countByIsAdminTrueAndIdNot(Long id);

    // Tìm kiếm theo phone - email - role
    @Query("""
        SELECT u FROM User u
        WHERE (:phone IS NULL OR u.phone LIKE %:phone%)
          AND (:email IS NULL OR u.email LIKE %:email%)
          AND (:roles IS NULL OR u.role IN :roles)
    """)
    Page<User> search(
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("roles") List<User.Role> roles,
            Pageable pageable
    );

    @Query("""
    SELECT u FROM User u
    WHERE u.enabled = true
      AND (
           LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    List<User> searchByEmailOrName(@Param("keyword") String keyword);
}
