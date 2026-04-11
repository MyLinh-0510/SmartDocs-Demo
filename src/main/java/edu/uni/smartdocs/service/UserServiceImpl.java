package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.opencsv.CSVReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Các phương thức dùng trong AdminController

    @Override
    public List<User> findAll() {

        return userRepository.findAll();
    }

    @Override
    public long countAdmins() {

        return userRepository.countByIsAdminTrue();
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone); // hoặc findByPhoneContaining, hoặc Optional...
    }

    @Override
    public void save(User user) {

        userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {

        return userRepository.findByEmail(email);
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElse(null);
    }



    @Override
    public void deleteById(Long id) {

        userRepository.deleteById(id);
    }

    //  Các phương thức dùng cho khôi phục mật khẩu

    @Override
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống."));

        String token = generateRandomHexToken(32);

        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        String resetLink = "http://localhost:8080/admin/account/reset-password?token=" + token;
        emailService.sendResetPasswordEmail(email, resetLink);

        System.out.println("📧 Email khôi phục mật khẩu đã được gửi thành công.");
    }

    @Override
    public User validateResetToken(String token) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ hoặc không tồn tại."));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn. Vui lòng yêu cầu lại.");
        }

        return user;
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        User user = validateResetToken(token);

        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private String generateRandomHexToken(int byteLength) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public Object countUsers() {
        return userRepository.count();
    }

    @Override
    public List<Integer> getMonthlyUserCounts() {
        int year = java.time.Year.now().getValue();
        List<Object[]> raw = userRepository.countUsersByMonth(year);

        List<Integer> result = new ArrayList<>(Collections.nCopies(12, 0));

        for (Object[] row : raw) {
            int month = ((Integer) row[0]) - 1;
            long count = (long) row[1];
            result.set(month, (int) count);
        }

        return result;
    }

    @Override
    public User getCurrentUser(java.security.Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getApprovers() {
        return userRepository.findByRole(User.Role.CEO);
    }

    @Override
    public void saveAdmin(User user) {

        if (user.isAdmin() || user.getRole() == User.Role.ADMIN) {

            long adminCount = userRepository.countByIsAdminTrue();

            if (adminCount >= 3) {
                throw new RuntimeException("Đã đạt giới hạn 3 tài khoản quản trị. Vui lòng kiểm tra lại.");
            }
        }

        userRepository.save(user);
    }

    @Override
    public void saveWithAdminLimit(User user, boolean isUpdate) {

        boolean newIsAdmin = user.isAdmin() || user.getRole() == User.Role.ADMIN;

        if (newIsAdmin) {

            long adminCount;

            if (isUpdate) {
                // 🔥 loại trừ chính user đang update
                adminCount = userRepository.countByIsAdminTrueAndIdNot(user.getId());
            } else {
                adminCount = userRepository.countByIsAdminTrue();
            }

            if (adminCount >= 3) {
                throw new RuntimeException(
                        "Đã đạt giới hạn 3 tài khoản quản trị. Không thể thêm admin mới."
                );
            }
        }

        userRepository.save(user);
    }

    @Transactional
    public void toggleLockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setEnabled(!user.isEnabled());

        userRepository.save(user);
    }

    @Override
    public Page<User> search(
            String phone,
            String email,
            List<String> roles,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").descending()
        );

        // Nếu roles rỗng → null để bỏ điều kiện IN
        List<User.Role> roleEnums = null;
        if (roles != null && !roles.isEmpty()) {
            roleEnums = roles.stream()
                    .map(User.Role::valueOf)
                    .toList();
        }

        // Chuẩn hóa input rỗng
        phone = (phone == null || phone.isBlank()) ? null : phone;
        email = (email == null || email.isBlank()) ? null : email;

        return userRepository.search(phone, email, roleEnums, pageable);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    // Upload và lưu file csv
    @Transactional
    public List<String> importUsersFromCsv(MultipartFile file) {

        List<String> errors = new ArrayList<>();
        List<User> usersToSave = new ArrayList<>();

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String[] line;
            boolean skipHeader = true;
            int row = 0;
            int dataRowCount = 0; // 👉 đếm số dòng dữ liệu thực tế

            while ((line = reader.readNext()) != null) {
                row++;

                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                // bỏ qua dòng trống hoàn toàn
                if (line.length == 0 || Arrays.stream(line).allMatch(String::isBlank)) {
                    continue;
                }

                dataRowCount++;

                if (line.length < 5) {
                    errors.add("Dòng " + row + ": Thiếu thông tin bắt buộc.");
                    continue;
                }

                String name = line[0].trim();
                String phone = line[1].trim();
                String email = line[2].trim().toLowerCase();
                String password = line[3].trim();
                String roleStr = line[4].trim();

                // ===== CHECK TRỐNG TỪNG FIELD =====
                if (name.isBlank()) {
                    errors.add("Dòng " + row + ": Tên người dùng không được để trống.");
                    continue;
                }

                if (phone.isBlank()) {
                    errors.add("Dòng " + row + ": Số điện thoại không được để trống.");
                    continue;
                }

                if (email.isBlank()) {
                    errors.add("Dòng " + row + ": Email không được để trống.");
                    continue;
                }

                if (password.isBlank()) {
                    errors.add("Dòng " + row + ": Mật khẩu không được để trống.");
                    continue;
                }

                if (roleStr.isBlank()) {
                    errors.add("Dòng " + row + ": Vai trò không được để trống.");
                    continue;
                }

                // ===== VALIDATE FORMAT =====
                if (!phone.matches("^\\d{10}$")) {
                    errors.add("Dòng " + row + ": Số điện thoại phải gồm đúng 10 chữ số.");
                    continue;
                }

                if (existsByPhone(phone)) {
                    errors.add("Dòng " + row + ": Số điện thoại đã tồn tại trong hệ thống.");
                    continue;
                }

                if (!email.matches("^[\\w-.]+@gmail\\.com$")) {
                    errors.add("Dòng " + row + ": Email phải là địa chỉ @gmail.com.");
                    continue;
                }

                if (existsByEmail(email)) {
                    errors.add("Dòng " + row + ": Email đã tồn tại trong hệ thống.");
                    continue;
                }

                if (password.length() < 6) {
                    errors.add("Dòng " + row + ": Mật khẩu phải có ít nhất 6 ký tự.");
                    continue;
                }

                User.Role role;
                try {
                    role = User.Role.valueOf(roleStr);
                } catch (Exception e) {
                    role = User.Role.EMPLOYEE;
                }

                User user = new User();
                user.setName(name);
                user.setPhone(phone);
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(password));
                user.setRole(role);
                user.syncAdminFromRole();
                user.setAvatar("default.jpg");

                usersToSave.add(user);
            }

            // ===== CSV KHÔNG CÓ DỮ LIỆU =====
            if (dataRowCount == 0) {
                errors.add("File CSV không có dữ liệu người dùng để import.");
            }

        } catch (Exception e) {
            errors.add("Không thể đọc file CSV. Vui lòng kiểm tra định dạng hoặc mã hóa UTF-8.");
        }

        // ===== CÓ LỖI → KHÔNG LƯU =====
        if (!errors.isEmpty()) {
            return errors;
        }

        // ===== KHÔNG LỖI → SAVE TOÀN BỘ =====
        userRepository.saveAll(usersToSave);

        return errors;
    }

    @Override
    public List<User> searchByEmailOrName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return userRepository.searchByEmailOrName(keyword);
    }

}
