package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.User;
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

    Optional<User> findByResetToken(String token);

    long countByIsAdminTrue();

    @Query("SELECT MONTH(u.createdAt), COUNT(u) FROM User u " +
            "WHERE YEAR(u.createdAt) = :year " +
            "GROUP BY MONTH(u.createdAt)")
    List<Object[]> countUsersByMonth(@Param("year") int year);

}
