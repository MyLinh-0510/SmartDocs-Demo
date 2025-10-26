package edu.uni.smartdocs.config;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.models.User.Role;
import edu.uni.smartdocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDefaultUsers() {
        return args -> {
            // ---------- Tạo admin mặc định ----------
            String adminEmail = "admin@gmail.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setName("Admin");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("123456")); // ✅ mã hóa
                admin.setRole(Role.ADMIN);
                admin.setAdmin(true); // ✅ nếu bạn dùng user.isAdmin() để phân quyền
                userRepository.save(admin);
                System.out.println("✅ Tài khoản admin mặc định đã được tạo: " + adminEmail);
            } else {
                System.out.println("✅ Tài khoản admin đã tồn tại: " + adminEmail);
            }

            // ---------- Tạo employee mặc định ----------
            String employeeEmail = "employee@gmail.com";
            if (userRepository.findByEmail(employeeEmail).isEmpty()) {
                User employee = new User();
                employee.setName("Employee");
                employee.setEmail(employeeEmail);
                employee.setPassword(passwordEncoder.encode("123456")); // ✅ mã hóa
                employee.setRole(Role.EMPLOYEE);
                employee.setAdmin(false); // ✅ rõ ràng hơn
                userRepository.save(employee);
                System.out.println("✅ Tài khoản nhân viên mặc định đã được tạo: " + employeeEmail);
            } else {
                System.out.println("✅ Tài khoản nhân viên đã tồn tại: " + employeeEmail);
            }
        };
    }
}
