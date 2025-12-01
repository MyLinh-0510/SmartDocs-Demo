package edu.uni.smartdocs.controllers;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/admin/account")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // ---------- REGISTER ----------
    @GetMapping("/register")
    public String getRegister() {
        return "admin/account/register";
    }

    @PostMapping("/register")
    public String postRegister(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               Model model) {

        // Kiểm tra các điều kiện
        if (name.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập họ tên");
            return "admin/account/register";
        }
        if (!email.matches("^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            model.addAttribute("error", "Email không hợp lệ");
            return "admin/account/register";
        }
        if (password.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải ít nhất 6 ký tự");
            return "admin/account/register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            return "admin/account/register";
        }
        if (userService.countAdmins() >= 3) {
            model.addAttribute("error", "Đã đạt giới hạn 3 tài khoản quản trị.");
            return "admin/account/register";
        }
        if (userService.findByEmail(email.toLowerCase()).isPresent()) {
            model.addAttribute("error", "Email đã tồn tại");
            return "admin/account/register";
        }

        // ✅ Nếu đến đây => hợp lệ, tiến hành lưu
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email.toLowerCase());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAdmin(true);
        newUser.setRole(User.Role.ADMIN);

        System.out.println(">>> Lưu tài khoản admin mới: " + newUser.getEmail());
        userService.save(newUser);
        System.out.println(">>> Lưu thành công!");

        model.addAttribute("success", "Tạo tài khoản admin thành công!");
        return "admin/account/login";
    }


    // ---------- LOGIN ----------
    @GetMapping("/login")
    public String showLogin(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            HttpServletRequest request,
                            HttpSession session,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Sai tài khoản hoặc mật khẩu.");
        }
        if (logout != null) {
            model.addAttribute("message", "Đăng xuất thành công.");
        }

        // ✅ Ghi nhớ URL gốc nếu có
        String referrer = request.getHeader("Referer");
        if (referrer != null && !referrer.contains("/login")) {
            session.setAttribute("redirectAfterLogin", referrer);
        }

        return "admin/account/login";
    }


    // ---------- PROFILE ----------
    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal,
                              @ModelAttribute("success") String successMessage) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        String username = principal.getName();
        User user = userService.findByEmail(username).orElse(null);

        if (user == null) {
            return "redirect:/admin/account/login";
        }

        model.addAttribute("user", user);

        model.addAttribute("rawPassword", user.getPassword()); // Đây là plaintext nếu bạn lưu plaintext

        // Nếu có thông báo thành công, truyền qua view
        if (successMessage != null && !successMessage.isEmpty()) {
            model.addAttribute("success", successMessage);
        }

        return "admin/account/profile";
    }

    // 👤 FORM CHỈNH SỬA THÔNG TIN
    @GetMapping("/profile-edit")
    public String editProfileForm(Model model, Principal principal) {
        if (principal == null) return "redirect:/admin/account/login";

        String email = principal.getName();
        User user = userService.findByEmail(email).orElse(null);

        if (user == null) return "redirect:/admin/account/login";

        model.addAttribute("user", user);
        return "admin/account/profile-edit";
    }

    @PostMapping("/profile-edit")
    public String updateProfile(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            Principal principal,
            RedirectAttributes redirect
    ) {
        if (principal == null) return "redirect:/admin/account/login";

        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/admin/account/login";


        // 1) VALIDATE TÊN
        if (name.isBlank()) {
            redirect.addFlashAttribute("error", "Họ tên không được để trống.");
            return "redirect:/admin/account/profile-edit";
        }

        // 2) VALIDATE EMAIL
        String newEmail = email.toLowerCase();

        if (!newEmail.matches("^[\\w-.]+@[\\w-]+\\.[A-Za-z]{2,}$")) {
            redirect.addFlashAttribute("error", "Email không hợp lệ.");
            return "redirect:/admin/account/profile-edit";
        }

        // Nếu đổi email → kiểm tra trùng
        if (!newEmail.equalsIgnoreCase(user.getEmail())) {
            if (userService.findByEmail(newEmail).isPresent()) {
                redirect.addFlashAttribute("error", "Email đã tồn tại trong hệ thống.");
                return "redirect:/admin/account/profile-edit";
            }
        }

        // 3) VALIDATE ĐỔI MẬT KHẨU (nếu nhập)
        if (currentPassword != null && !currentPassword.isBlank()) {

            // Kiểm tra mật khẩu cũ
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirect.addFlashAttribute("error", "Mật khẩu hiện tại không đúng.");
                return "redirect:/admin/account/profile-edit";
            }

            // Kiểm tra độ dài
            if (newPassword.length() < 6) {
                redirect.addFlashAttribute("error", "Mật khẩu mới phải từ 6 ký tự.");
                return "redirect:/admin/account/profile-edit";
            }

            // Kiểm tra khớp
            if (!newPassword.equals(confirmPassword)) {
                redirect.addFlashAttribute("error", "Xác nhận mật khẩu không khớp.");
                return "redirect:/admin/account/profile-edit";
            }

            // Cập nhật mật khẩu mới
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        // 4) CẬP NHẬT TÊN + EMAIL
        user.setName(name);
        user.setEmail(newEmail);
        userService.save(user);

        redirect.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        return "redirect:/admin/account/profile";
    }


    // ---------- FORGOT PASSWORD ----------
    @GetMapping("/forgot-password")
    public String showForgotPassword(Model model) {
        return "admin/account/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        Optional<User> opt = userService.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) {
            model.addAttribute("error", "Email không tồn tại trong hệ thống.");
        } else {
            userService.initiatePasswordReset(email.trim().toLowerCase());
            model.addAttribute("message", "Hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn. Vui lòng kiểm tra lại.");
        }
        return "admin/account/forgot-password";
    }

    // ---------- RESET PASSWORD ----------
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        try {
            userService.validateResetToken(token);
            model.addAttribute("token", token);
            return "admin/account/reset-password";
        } catch (Exception ex) {
            model.addAttribute("error", "Token không hợp lệ hoặc đã hết hạn.");
            return "admin/account/forgot-password";
        }
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            model.addAttribute("token", token);
            return "admin/account/reset-password";
        }

        try {
            userService.resetPassword(token, newPassword);
            model.addAttribute("success", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
            return "admin/account/login";
        } catch (Exception ex) {
            model.addAttribute("error", "Token không hợp lệ hoặc đã hết hạn.");
            model.addAttribute("token", token);
            return "admin/account/reset-password";
        }
    }

}
