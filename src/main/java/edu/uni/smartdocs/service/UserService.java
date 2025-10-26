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
    void initiatePasswordReset(String email);
    User validateResetToken(String token);
    void resetPassword(String token, String newPassword);
}
