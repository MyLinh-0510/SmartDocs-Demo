package edu.uni.smartdocs.controllers;

import edu.uni.smartdocs.models.Category;
import edu.uni.smartdocs.models.ContactMessage;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.ContactMessageRepository;
import edu.uni.smartdocs.service.CategoryService;
import edu.uni.smartdocs.service.DocumentService;
import edu.uni.smartdocs.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;


@Controller
@RequestMapping("/site")
@RequiredArgsConstructor
public class SiteController {

    private final DocumentService documentService;
    private final UserService userService;
    private final CategoryService categoryService;

    // Thêm currentPath để active menu
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute
    public void addCategoriesToAllPages(Model model) {
        List<Category> categories = categoryService.findAll();
        model.addAttribute("allCategories", categories);

        // Debug nhẹ để bạn biết nó đang chạy
        System.out.println("SiteController → Đã thêm " + categories.size() + " danh mục vào Model");
    }

    // 🏠 TRANG CHỦ
    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String home(Model model) {
        List<Document> docs = documentService.findVisibleDocuments();
        if (docs == null) docs = new ArrayList<>();
        model.addAttribute("latestDocs", docs);
        model.addAttribute("totalDocs", docs.size());
        model.addAttribute("totalUsers", userService.countUsers());
        return "site/home";
    }

    // 🔍 TRANG TÌM KIẾM
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String search(@RequestParam(required = false) String keyword, Model model) {
        List<Document> docs = (keyword == null || keyword.isBlank())
                ? documentService.findVisibleDocuments()
                : documentService.findByKeyword(keyword);

        model.addAttribute("keyword", keyword);
        model.addAttribute("results", docs);
        return "site/search";
    }

    // 📄 CHI TIẾT TÀI LIỆU
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String detail(@PathVariable Long id, Model model) {
        Document document = documentService.findById(id).orElse(null);
        model.addAttribute("document", document);
        return "site/detail";
    }

    // 👤 TRANG THÔNG TIN CÁ NHÂN
    @GetMapping("/profile-user")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String profile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        String username = principal.getName();

        // Nếu UserService trả về Optional<User>
        User user = userService.findByEmail(username).orElse(null);

        model.addAttribute("user", user);
        return "site/profile-user";
    }

    // 👤 FORM CHỈNH SỬA THÔNG TIN
    @GetMapping("/edit-profile")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String editProfileForm(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        String username = principal.getName();
        User user = userService.findByEmail(username).orElse(null);

        if (user == null) {
            return "redirect:/site/profile_user";
        }

        model.addAttribute("user", user);
        return "site/edit-profile";
    }

    // 💾 XỬ LÝ LƯU THAY ĐỔI
    @PostMapping("/edit-profile")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String saveProfile(@ModelAttribute("user") User updatedUser, Principal principal) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        String username = principal.getName();
        User existingUser = userService.findByEmail(username).orElse(null);

        if (existingUser == null) {
            return "redirect:/site/profile_user";
        }

        // Chỉ cho phép sửa tên
        existingUser.setName(updatedUser.getName());

        userService.save(existingUser);
        return "site/profile-user";
    }

    // 🔒 TRANG ĐỔI MẬT KHẨU (USER)
    @GetMapping("/change-password-user")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String showChangePasswordFormUser() {
        return "site/change-password-user";  // file riêng cho user
    }

    // 💾 XỬ LÝ ĐỔI MẬT KHẨU (USER)
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
            return "site/change-password-user";
        }

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ thông tin.");
            return "site/change-password-user";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "site/change-password-user";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return "site/change-password-user";
        }

        if (!userService.passwordMatches(currentPassword, user.getPassword())) {
            model.addAttribute("error", "Mật khẩu hiện tại không đúng.");
            return "site/change-password-user";
        }

        user.setPassword(userService.encodePassword(newPassword));
        userService.save(user);

        model.addAttribute("success", "Đổi mật khẩu thành công!");
        return "admin/account/login";
    }

    //Trang tài liệu
    @GetMapping("/documents-user")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String myDocuments(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null) {
            return "redirect:/site/home";
        }

        User.Role userRole = currentUser.getRole();

        List<Document> allVisibleDocs = documentService.findVisibleDocuments();

        List<Document> myDocs = allVisibleDocs.stream()
                .filter(doc -> doc.getVisibleToRoles() != null && doc.getVisibleToRoles().contains(userRole))
                .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
                .toList();

        model.addAttribute("documents", myDocs);
        model.addAttribute("pageTitle", "Tài liệu của tôi");
        model.addAttribute("totalMyDocs", myDocs.size()); // thêm để debug

        return "site/documents-user";
    }


}
