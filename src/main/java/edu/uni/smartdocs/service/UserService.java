package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> findAll();
    long countAdmins();
    void save(User user);
    void deleteById(Long id);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByPhone(String phone);
    void initiatePasswordReset(String email);
    User validateResetToken(String token);
    void resetPassword(String token, String newPassword);
    boolean existsByEmail(String email);
    boolean passwordMatches(String rawPassword, String encodedPassword);
    String encodePassword(String rawPassword);

    Object countUsers();

    List<Integer> getMonthlyUserCounts();

}
