package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

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

    User getById(Long id);

    Object countUsers();

    List<Integer> getMonthlyUserCounts();

    User getCurrentUser(java.security.Principal principal);

    List<User> getApprovers();

    void saveAdmin(User user);

    void saveWithAdminLimit(User user, boolean isUpdate);

    Page<User> findAll(Pageable pageable);

    void toggleLockUser(Long userId);

    Page<User> search(
            String phone,
            String email,
            List<String> roles,
            int page,
            int size
    );

    List<String> importUsersFromCsv(MultipartFile file);

    List<User> searchByEmailOrName(String keyword);

}
