package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;

@Controller
@RequestMapping("/user/account")   // ← Nhất quán và sạch sẽ (không dùng accountu nữa)
@RequiredArgsConstructor
public class UserAuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // ====================== LOGIN USER ======================
    @GetMapping("/login")
    public String showUserLogin(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                HttpSession session,
                                Model model) {

        if (error != null) {
            Object errMsg = session.getAttribute("error");
            if (errMsg != null) {
                model.addAttribute("error", errMsg.toString());
                session.removeAttribute("error");
            }
        }

        if (logout != null) {
            model.addAttribute("message", "Đăng xuất thành công.");
        }

        return "user/account/login";     // Template login dành cho User
    }

    // ======================== TIỆN ÍCH GIỮ PASSWORD ========================
    private void keepPasswordFields(Model model, String current, String news, String confirm) {
        model.addAttribute("currentPassword", current);
        model.addAttribute("newPassword", news);
        model.addAttribute("confirmPassword", confirm);
    }

    // ======================== PROFILE ========================
    @GetMapping("/profile-user")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String profile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/user/account/login";
        }

        User user = userService.findByEmail(principal.getName()).orElse(null);
        model.addAttribute("user", user);
        return "user/account/profile-user";
    }

    // ======================== EDIT FORM ========================
    @GetMapping("/edit-profile")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String editProfileForm(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/user/account/login";
        }

        User user = userService.findByEmail(principal.getName()).orElse(null);
        model.addAttribute("user", user);
        return "user/account/edit-profile";
    }

    // ======================== SAVE PROFILE ========================
    @PostMapping("/edit-profile")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String saveProfile(
            @ModelAttribute("user") User updatedUser,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            Principal principal,
            Model model) throws IOException {

        if (principal == null) {
            return "redirect:/user/account/login";
        }

        User existingUser = userService.findByEmail(principal.getName()).orElse(null);
        if (existingUser == null) {
            return "redirect:/user/account/login";
        }

        boolean isChanged = false;
        boolean passwordChanged = false;

        // ================= NAME =================
        if (updatedUser.getName() == null || updatedUser.getName().isBlank()) {
            model.addAttribute("error", "Tên không được để trống");
            model.addAttribute("user", existingUser);
            keepPasswordFields(model, currentPassword, newPassword, confirmPassword);
            return "user/account/edit-profile";
        }

        if (!updatedUser.getName().equals(existingUser.getName())) {
            existingUser.setName(updatedUser.getName());
            isChanged = true;
        }

        // ================= PHONE =================
        if (!updatedUser.getPhone().equals(existingUser.getPhone())) {
            if (!updatedUser.getPhone().matches("^\\d{10}$")) {
                model.addAttribute("error", "Số điện thoại phải gồm 10 số");
                model.addAttribute("user", existingUser);
                keepPasswordFields(model, currentPassword, newPassword, confirmPassword);
                return "user/account/edit-profile";
            }

            if (userService.existsByPhone(updatedUser.getPhone())) {
                model.addAttribute("error", "Số điện thoại đã tồn tại");
                model.addAttribute("user", existingUser);
                keepPasswordFields(model, currentPassword, newPassword, confirmPassword);
                return "user/account/edit-profile";
            }

            existingUser.setPhone(updatedUser.getPhone());
            isChanged = true;
        }

        // ================= AVATAR =================
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + avatarFile.getOriginalFilename();

            Path path = Paths.get("uploads/avatars");
            Files.createDirectories(path);

            Files.copy(avatarFile.getInputStream(), path.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            existingUser.setAvatar(fileName);
            isChanged = true;
        }

        // ================= PASSWORD =================
        boolean hasPasswordInput = (currentPassword != null && !currentPassword.isBlank()) ||
                (newPassword != null && !newPassword.isBlank()) ||
                (confirmPassword != null && !confirmPassword.isBlank());

        if (hasPasswordInput) {
            boolean hasError = false;

            if (currentPassword == null || currentPassword.isBlank()) {
                model.addAttribute("errorCurrentPassword", "Vui lòng nhập mật khẩu hiện tại");
                hasError = true;
            }
            if (newPassword == null || newPassword.isBlank()) {
                model.addAttribute("errorNewPassword", "Vui lòng nhập mật khẩu mới");
                hasError = true;
            }
            if (confirmPassword == null || confirmPassword.isBlank()) {
                model.addAttribute("errorConfirmPassword", "Vui lòng xác nhận mật khẩu");
                hasError = true;
            }

            if (hasError) {
                model.addAttribute("user", existingUser);
                keepPasswordFields(model, currentPassword, newPassword, confirmPassword);
                return "user/account/edit-profile";
            }

            if (!userService.passwordMatches(currentPassword, existingUser.getPassword())) {
                model.addAttribute("errorCurrentPassword", "Mật khẩu hiện tại không đúng");
                model.addAttribute("user", existingUser);
                keepPasswordFields(model, currentPassword, newPassword, confirmPassword);
                return "user/account/edit-profile";
            }

            if (newPassword.length() < 6) {
                model.addAttribute("errorNewPassword", "Mật khẩu phải có ít nhất 6 ký tự");
                model.addAttribute("user", existingUser);
                keepPasswordFields(model, currentPassword, newPassword, confirmPassword);
                return "user/account/edit-profile";
            }

            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("errorConfirmPassword", "Mật khẩu xác nhận không khớp");
                model.addAttribute("user", existingUser);
                keepPasswordFields(model, currentPassword, newPassword, confirmPassword);
                return "user/account/edit-profile";
            }

            // Đổi mật khẩu thành công
            existingUser.setPassword(userService.encodePassword(newPassword));
            isChanged = true;
            passwordChanged = true;
        }

        if (!isChanged) {
            model.addAttribute("info", "Không có thay đổi nào được thực hiện");
            model.addAttribute("user", existingUser);
            keepPasswordFields(model, "", "", "");
            return "user/account/edit-profile";
        }

        userService.save(existingUser);

        // Nếu đổi password → buộc đăng nhập lại
        if (passwordChanged) {
            return "redirect:/user/account/login?logout";   // Thêm ?logout để hiển thị thông báo
        }

        // Chỉ edit thông tin khác → ở lại trang edit
        model.addAttribute("success", "Cập nhật thông tin thành công");
        model.addAttribute("user", existingUser);
        keepPasswordFields(model, "", "", "");

        return "user/account/profile-user";
    }
}