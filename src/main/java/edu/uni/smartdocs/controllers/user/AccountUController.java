package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/user/accountu")
@RequiredArgsConstructor
public class AccountUController {

    private final UserService userService;

    //Profile
    @GetMapping("/profile-user")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String profile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        String username = principal.getName();
        User user = userService.findByEmail(username).orElse(null);

        model.addAttribute("user", user);
        return "user/accountu/profile-user";
    }

    //Form edit information
    @GetMapping("/edit-profile")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String editProfileForm(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        String username = principal.getName();
        User user = userService.findByEmail(username).orElse(null);

        if (user == null) {
            return "redirect:/user/accountu/profile_user";
        }

        model.addAttribute("user", user);
        return "user/accountu/edit-profile";
    }

    //Process save change
    @PostMapping("/edit-profile")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String saveProfile(@ModelAttribute("user") User updatedUser,
                              Principal principal,
                              Model model) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        String username = principal.getName();
        User existingUser = userService.findByEmail(username).orElse(null);

        if (existingUser == null) {
            return "redirect:/site/accountu/profile_user";
        }

        //Validate name
        if (updatedUser.getName() == null || updatedUser.getName().isBlank()) {
            model.addAttribute("error", "Tên không được để trống.");
            model.addAttribute("user", existingUser);
            return "user/accountu/edit-profile";
        }

        //Validate phone
        String phone = updatedUser.getPhone();

        if (phone == null || !phone.matches("^\\d{10}$")) {
            model.addAttribute("error", "Số điện thoại phải gồm đúng 10 chữ số.");
            model.addAttribute("user", existingUser);
            return "user/accountu/edit-profile";
        }
        if (!phone.equals(existingUser.getPhone())) {   // chỉ kiểm tra nếu có thay đổi
            if (userService.existsByPhone(phone)) {
                model.addAttribute("error",
                        "Số điện thoại " + phone + " đã được sử dụng bởi tài khoản khác.");
                model.addAttribute("user", existingUser);
                return "user/accountu/edit-profile";
            }
        }

        //Update data
        existingUser.setName(updatedUser.getName());
        existingUser.setPhone(phone);

        userService.save(existingUser);

        model.addAttribute("success", "Cập nhật thông tin thành công!");
        model.addAttribute("user", existingUser);

        return "user/accountu/profile-user";
    }

    //Change password
    @GetMapping("/change-password-user")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String showChangePasswordFormUser() {
        return "user/accountu/change-password-user";
    }

    //Process password change
    @PostMapping("/change-password-user")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String changePasswordUser(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Principal principal,
            Model model
    ) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        String email = principal.getName();
        User user = userService.findByEmail(email).orElse(null);

        if (user == null) {
            model.addAttribute("error", "Không tìm thấy người dùng.");
            return "user/accountu/change-password-user";
        }

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ thông tin.");
            return "user/accountu/change-password-user";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "user/accountu/change-password-user";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return "user/accountu/change-password-user";
        }

        if (!userService.passwordMatches(currentPassword, user.getPassword())) {
            model.addAttribute("error", "Mật khẩu hiện tại không đúng.");
            return "user/accountu/change-password-user";
        }

        user.setPassword(userService.encodePassword(newPassword));
        userService.save(user);

        model.addAttribute("success", "Đổi mật khẩu thành công!");
        return "admin/account/login";
    }
}
