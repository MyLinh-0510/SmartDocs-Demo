package edu.uni.smartdocs.controllers.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.uni.smartdocs.models.Contact;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.ContactMessageRepository;
import edu.uni.smartdocs.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final DocumentService documentService;
    private final CategoryService categoryService;


    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ContactService contactService;

    // ---------- DASHBOARD ----------
    @GetMapping("/dashboard")
    public String dashboard(Model model) throws JsonProcessingException {

        // Danh sách nhãn cho biểu đồ
        List<String> labels = List.of(
                "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        );

        // ----- Thống kê tổng quan -----
        model.addAttribute("stats", Map.of(
                "totalUsers", userService.countUsers(),
                "totalOrders", 287L,
                "totalDocuments", documentService.count(),
                "totalCategories", categoryService.findAll().size(),
                "totalFeedbacks", contactService.count()
        ));

        // ----- Biểu đồ Users theo tháng -----
        model.addAttribute("chartDataJson",
                objectMapper.writeValueAsString(
                        Map.of(
                                "labels", labels,
                                "newUsers", userService.getMonthlyUserCounts()
                        )
                )
        );

        // ----- Tài liệu được tải nhiều nhất -----
        model.addAttribute("bestSellingProductsJson",
                objectMapper.writeValueAsString(
                        List.of(
                                Map.of("name", "Hợp đồng lao động", "sales", 189),
                                Map.of("name", "Quy chế công ty", "sales", 142),
                                Map.of("name", "Nội quy an toàn", "sales", 98),
                                Map.of("name", "Hướng dẫn sử dụng", "sales", 76),
                                Map.of("name", "Báo cáo tài chính", "sales", 65)
                        )
                )
        );

        injectUser(model);
        return "admin/dashboard";
    }


    // ---------- USERS LIST ----------
    @GetMapping("/users/index")
    public String listUsers(Model model) {
        if (!hasAdminRole()) {
            model.addAttribute("error", "Bạn không có quyền truy cập.");
            return "admin/account/login";
        }

        model.addAttribute("users", userService.findAll());
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
                             @RequestParam String phone,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             @RequestParam(required = false) String isAdmin,
                             Model model) {

        // Kiểm tra role admin
        if (!hasAdminRole()) {
            model.addAttribute("error", "Bạn không có quyền truy cập.");
            return "admin/account/login";
        }

        // Validate dữ liệu
        if (name == null || name.isBlank()) {
            model.addAttribute("error", "Tên không được để trống");
            model.addAttribute("newUser", new User());
            return "admin/users/create";
        }
        if (email == null || !email.matches("^[\\w-.]+@gmail\\.com$")) {
            model.addAttribute("error", "Email phải là địa chỉ Gmail hợp lệ (vd: ten@gmail.com)");
            model.addAttribute("newUser", new User());
            return "admin/users/create";
        }

        //Kiểm tra số điện thoại
        if (phone != null) phone = phone.trim();

        if (phone == null || !phone.matches("^\\d{10}$")) {
            model.addAttribute("error", "Số điện thoại phải gồm đúng 10 chữ số!");
            model.addAttribute("newUser", new User());
            return "admin/users/create";
        }

        if (userService.existsByPhone(phone)) {
            model.addAttribute("error", "Số điện thoại đã được sử dụng bởi tài khoản khác!");
            model.addAttribute("newUser", new User());
            return "admin/users/create";
        }


        // Mật khẩu không được trống
        if (password == null || password.isBlank()) {
            model.addAttribute("error", "Mật khẩu không được để trống");
            model.addAttribute("newUser", new User());
            return "admin/users/create";
        }

        // Mật khẩu phải dài hơn 6 ký tự
        if (password.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự");
            model.addAttribute("newUser", new User());
            return "admin/users/create";
        }


        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            model.addAttribute("newUser", new User());
            return "admin/users/create";
        }

        if (userService.existsByEmail(email)) {
            model.addAttribute("error", "Email đã tồn tại");
            model.addAttribute("newUser", new User());
            return "admin/users/create";
        }

        // Lưu user
        User newUser = new User();
        newUser.setName(name);
        newUser.setPhone(phone);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAdmin(isAdmin != null);

        userService.save(newUser);

        // 🟢 THÀNH CÔNG → TRẢ VỀ TRANG LIST NGAY (KHÔNG REDIRECT)
        model.addAttribute("success", "Tạo tài khoản thành công!");
        model.addAttribute("users", userService.findAll());
        injectUser(model);

        return "admin/users/index";  // load trực tiếp trang danh sách
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
    public String updateUser(
            @PathVariable Long id,
            @ModelAttribute("editingUser") User userForm,
            BindingResult result,
            @RequestParam(value = "isAdmin", required = false) String isAdmin,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        // Kiểm tra quyền
        if (principal == null || !hasAdminRole()) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền!");
            return "redirect:/admin/users";
        }

        // Kiểm tra user tồn tại
        User editingUser = userService.findById(id).orElse(null);
        if (editingUser == null) {
            redirectAttributes.addFlashAttribute("error", "Người dùng không tồn tại!");
            return "redirect:/admin/users";
        }

        String emailLower = userForm.getEmail().trim().toLowerCase();

        // Kiểm tra định dạng email
        if (!emailLower.endsWith("@gmail.com") && !emailLower.endsWith("@yopmail.com")) {
            redirectAttributes.addFlashAttribute("error", "Email chỉ được phép dùng @gmail.com hoặc @yopmail.com!");
            redirectAttributes.addFlashAttribute("editingUser", userForm);
            return "redirect:/admin/users/edit/" + id;
        }

        // Kiểm tra trùng email
        if (!editingUser.getEmail().equalsIgnoreCase(emailLower)
                && userService.existsByEmail(emailLower)) {

            redirectAttributes.addFlashAttribute("error", "Email '" + emailLower + "' đã được sử dụng!");
            redirectAttributes.addFlashAttribute("editingUser", userForm);
            return "redirect:/admin/users/edit/" + id;
        }

        String phone = userForm.getPhone();

        if (phone != null) {
            phone = phone.trim();
        }

        // Validate định dạng
        if (phone != null && !phone.matches("^\\d{10}$")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại phải gồm 10 chữ số!");
            redirectAttributes.addFlashAttribute("editingUser", userForm);
            return "redirect:/admin/users/edit/" + id;
        }

        // Kiểm tra trùng số điện thoại (chỉ khi thay đổi)
        String oldPhone = editingUser.getPhone() != null ? editingUser.getPhone().trim() : null;

        if (phone != null && !phone.equals(oldPhone)) {
            if (userService.existsByPhone(phone)) {
                redirectAttributes.addFlashAttribute("error",
                        "Số điện thoại " + phone + " đã được sử dụng bởi tài khoản khác!");
                redirectAttributes.addFlashAttribute("editingUser", userForm);
                return "redirect:/admin/users/edit/" + id;
            }
        }

        // Cập nhật thông tin
        editingUser.setName(userForm.getName().trim());
        editingUser.setPhone(phone != null ? phone.trim() : null);
        editingUser.setEmail(emailLower);
        editingUser.setAdmin(isAdmin != null);

        userService.save(editingUser);

        redirectAttributes.addFlashAttribute("success", "Cập nhật thành công!");

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

    // ---------- REDIRECT ROOT ----------
    @GetMapping
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }

}
