package edu.uni.smartdocs.controllers;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // chính là username trong Spring Security

        User user = userService.findByEmail(email).orElse(null);
        model.addAttribute("user", user);

        return "admin/account/profile";
    }


    // ---------- CHANGE PASSWORD ----------
    @GetMapping("/change-password")
    public String getChangePassword(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);

        // Hiển thị thông báo nếu có
        String error = (String) session.getAttribute("error");
        String success = (String) session.getAttribute("success");
        model.addAttribute("error", error);
        model.addAttribute("success", success);

        // Xóa thông báo sau khi hiển thị (tương tự flash message)
        session.removeAttribute("error");
        session.removeAttribute("success");

        return "admin/account/change-password";
    }

    // Xử lý đổi mật khẩu
    @PostMapping("/change-password")
    public String postChangePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            Authentication authentication
    ) {
        // ✅ Lấy email người dùng đang đăng nhập từ Security Context
        String email = authentication.getName();

        System.out.println("Đang đăng nhập: " + authentication.getName());


        // Lấy thông tin user từ DB
        Optional<User> optionalUser = userService.findByEmail(email);
        if (optionalUser.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy người dùng.");
            return "admin/account/change-password";
        }

        User dbUser = optionalUser.get();

        // ✅ Kiểm tra hợp lệ
        if (currentPassword.isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập mật khẩu hiện tại.");
            return "admin/account/change-password";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return "admin/account/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Xác nhận mật khẩu không khớp.");
            return "admin/account/change-password";
        }

        if (!passwordEncoder.matches(currentPassword, dbUser.getPassword())) {
            model.addAttribute("error", "Mật khẩu hiện tại không đúng.");
            return "admin/account/change-password";
        }

        // ✅ Cập nhật mật khẩu mới
        dbUser.setPassword(passwordEncoder.encode(newPassword));
        userService.save(dbUser);

        // ✅ Chuyển về trang login sau khi đổi mật khẩu
        return "redirect:/admin/account/login?changed=true";
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
