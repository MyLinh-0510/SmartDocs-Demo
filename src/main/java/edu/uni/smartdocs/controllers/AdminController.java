package edu.uni.smartdocs.controllers;

import edu.uni.smartdocs.models.Category;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.CategoryService;
import edu.uni.smartdocs.service.DocumentService;
import edu.uni.smartdocs.service.FileTypeService;
import edu.uni.smartdocs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;


    // ---------- DASHBOARD ----------
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        if (!hasAdminRole()) {
            model.addAttribute("error", "Bạn không có quyền truy cập.");
            return "admin/account/login";
        }

        model.addAttribute("stats", java.util.Map.of(
                "totalUsers", userService.findAll().size(),
                "totalOrders", 150,
                "totalRevenue", 120_000_000,
                "totalDocuments", 45
        ));

        model.addAttribute("bestSellingProducts", java.util.List.of(
                java.util.Map.of("name", "Áo thun", "sales", 80),
                java.util.Map.of("name", "Giày thể thao", "sales", 65),
                java.util.Map.of("name", "Ba lô", "sales", 30)
        ));

        model.addAttribute("chartData", java.util.Map.of(
                "labels", java.util.List.of("T1", "T2", "T3", "T4"),
                "newUsers", java.util.List.of(10, 20, 30, 40),
                "revenue", java.util.List.of(10_000_000, 25_000_000, 30_000_000, 40_000_000)
        ));

        injectUser(model);
        return "admin/dashboard";
    }

    // ---------- USERS LIST ----------
    @GetMapping("users/index")
    public String listUsers(Model model,
                            @RequestParam(value = "success", required = false) Boolean success,
                            @RequestParam(value = "action", required = false) String action,
                            @RequestParam(value = "error", required = false) String error) {
        if (!hasAdminRole()) {
            model.addAttribute("error", "Bạn không có quyền truy cập.");
            return "admin/account/login";
        }

        model.addAttribute("users", userService.findAll());
        model.addAttribute("success", success != null && success);
        model.addAttribute("action", action);
        model.addAttribute("error", error);
        injectUser(model);
        return "admin/users/index";
    }

    // ---------- CREATE USER ----------
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        if (!hasAdminRole()) {
            model.addAttribute("error", "Bạn không có quyền truy cập.");
            return "admin/account/login";
        }

        model.addAttribute("newUser", new User());
        injectUser(model);
        return "admin/users/create";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam(required = false) String isAdmin,
                             RedirectAttributes redirectAttributes) {
        if (!hasAdminRole()) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập.");
            return "redirect:/admin/account/login";
        }

        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Tên không được để trống");
            return "redirect:/admin/users/create";
        }

        if (!isValidEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email không hợp lệ");
            return "redirect:/admin/users/create";
        }

        if (password == null || password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu không được để trống");
            return "redirect:/admin/users/create";
        }

        if (userService.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại");
            return "redirect:/admin/users/create";
        }

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email.toLowerCase());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAdmin(isAdmin != null && (isAdmin.equals("on") || isAdmin.equals("true")));

        userService.save(newUser);

        redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành công!");
        return "redirect:/admin/users/index";
    }


    // ---------- SHOW EDIT FORM ----------
    @GetMapping("/users/edit/{id}")
    public String showEditUser(@PathVariable Long id, Model model) {
        if (!hasAdminRole()) {
            model.addAttribute("error", "Bạn không có quyền truy cập.");
            return "admin/account/login";
        }

        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Người dùng không tồn tại");
            model.addAttribute("users", userService.findAll());
            injectUser(model);
            return "admin/users/index";
        }

        //model.addAttribute("userToEdit", opt.get());
        model.addAttribute("editingUser", opt.get());
        injectUser(model);
        return "admin/users/edit";
    }

    // ---------- UPDATE USER ----------
    @PostMapping("/users/edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam String email,
                             @RequestParam(required = false) String isAdmin,
                             RedirectAttributes redirectAttributes) {
        if (!hasAdminRole()) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập.");
            return "redirect:/admin/account/login";
        }

        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Người dùng không tồn tại");
            return "redirect:/admin/users/index";
        }

        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Tên không được để trống");
            return "redirect:/admin/users/edit/" + id;
        }

        if (!isValidEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email không hợp lệ");
            return "redirect:/admin/users/edit/" + id;
        }

        User user = opt.get();
        user.setName(name);
        user.setEmail(email.toLowerCase());
        user.setAdmin(isAdmin != null && (isAdmin.equals("on") || isAdmin.equals("true")));
        userService.save(user);

        redirectAttributes.addFlashAttribute("success", "Cập nhật thành công");
        return "redirect:/admin/users/index";
    }

    // ---------- DELETE USER ----------
    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, Model model) {
        if (!hasAdminRole()) {
            model.addAttribute("error", "Bạn không có quyền truy cập.");
            return "admin/account/login";
        }

        try {
            userService.deleteById(id);
            model.addAttribute("success", "Người dùng đã được xóa");
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute("error", "Lỗi khi xóa người dùng");
        }

        model.addAttribute("users", userService.findAll());
        injectUser(model);
        return "admin/users/index";
    }

    // ---------- UTILS ----------
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean hasAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void injectUser(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("user", auth.getPrincipal());
    }

    // ---------- CATEGORY ----------
    private final CategoryService categoryservice;

    @GetMapping("/categories/index")
    public String listCategory(Model model) {
        model.addAttribute("categories", categoryservice.findAll());
        return "admin/categories/index";
    }

    @GetMapping("/categories/create")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/categories/create";
    }

    @PostMapping("/categories/save")
    public String save(@ModelAttribute Category category) {
        categoryservice.save(category);
        return "redirect:/admin/categories/index";
    }

    @PostMapping("/categories/delete/{id}")
    public String delete(@PathVariable Long id) {
        categoryservice.delete(id);
        return "redirect:/admin/categories/index";
    }


    // ---------- REDIRECT ROOT ----------
    @GetMapping
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }

}
