package edu.uni.smartdocs.controllers.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.uni.smartdocs.config.WebConfig;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.security.Principal;
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
    private final LogDownloadService logDownloadService;


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
                        logDownloadService.getTop5Downloaded()
                )
        );

        injectUser(model);
        return "admin/dashboard";
    }


    // ---------- USERS LIST ----------
    @GetMapping("/users/index")
    public String listUsers(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) List<String> roles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        if (!hasAdminRole()) {
            model.addAttribute("error", "Bạn không có quyền truy cập.");
            return "admin/account/login";
        }

        Page<User> userPage = userService.search(
                phone,
                email,
                roles,
                page,
                size
        );

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("size", size);

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

        User user = new User();
        user.setAvatar("default.jpg");

        model.addAttribute("newUser", user);
        injectUser(model);
        return "admin/users/create";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam("avatarFile") MultipartFile avatarFile,
                             @RequestParam String name,
                             @RequestParam String phone,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             @RequestParam String role,
                             Model model) {

        // Check quyền
        if (!hasAdminRole()) {
            model.addAttribute("error", "Bạn không có quyền truy cập.");
            return "admin/account/login";
        }

        // giữ laji data form
        User formUser = new User();
        formUser.setName(name);
        formUser.setPhone(phone);
        formUser.setEmail(email);
        formUser.setAvatar("default.jpg");

        try {
            formUser.setRole(User.Role.valueOf(role));
        } catch (Exception e) {
            formUser.setRole(User.Role.EMPLOYEE);
        }

        // validate
        if (name == null || name.isBlank()) {
            model.addAttribute("error", "Tên không được để trống");
            model.addAttribute("newUser", formUser);
            injectUser(model);
            return "admin/users/create";
        }

        if (email == null || !email.matches("^[\\w-.]+@gmail\\.com$")) {
            model.addAttribute("error", "Email phải là Gmail hợp lệ (vd: ten@gmail.com)");
            model.addAttribute("newUser", formUser);
            injectUser(model);
            return "admin/users/create";
        }

        if (phone != null) phone = phone.trim();

        if (phone == null || !phone.matches("^\\d{10}$")) {
            model.addAttribute("error", "Số điện thoại phải gồm đúng 10 chữ số!");
            model.addAttribute("newUser", formUser);
            injectUser(model);
            return "admin/users/create";
        }

        if (userService.existsByPhone(phone)) {
            model.addAttribute("error", "Số điện thoại đã được sử dụng!");
            model.addAttribute("newUser", formUser);
            injectUser(model);
            return "admin/users/create";
        }

        if (password == null || password.isBlank()) {
            model.addAttribute("error", "Mật khẩu không được để trống");
            model.addAttribute("newUser", formUser);
            injectUser(model);
            return "admin/users/create";
        }

        if (password.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự");
            model.addAttribute("newUser", formUser);
            injectUser(model);
            return "admin/users/create";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            model.addAttribute("newUser", formUser);
            injectUser(model);
            return "admin/users/create";
        }

        if (userService.existsByEmail(email)) {
            model.addAttribute("error", "Email đã tồn tại");
            model.addAttribute("newUser", formUser);
            injectUser(model);
            return "admin/users/create";
        }

        // tạo user
        User newUser = new User();
        newUser.setName(name.trim());
        newUser.setPhone(phone.trim());
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(User.Role.valueOf(role));
        newUser.syncAdminFromRole();

        // upload avatar
        String avatarFileName = "default.jpg";

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String contentType = avatarFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    model.addAttribute("error", "File upload phải là hình ảnh!");
                    model.addAttribute("newUser", formUser);
                    injectUser(model);
                    return "admin/users/create";
                }

                String uploadDir = WebConfig.UPLOAD_ROOT + "avatars/";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String originalName = avatarFile.getOriginalFilename();
                String fileName = generateUniqueFileName(uploadDir, originalName);

                File saveFile = new File(dir, fileName);
                avatarFile.transferTo(saveFile);

                avatarFileName = fileName;
                formUser.setAvatar(fileName);


            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("error", "Upload avatar thất bại!");
                model.addAttribute("newUser", formUser);
                injectUser(model);
                return "admin/users/create";
            }
        }

        newUser.setAvatar(avatarFileName);

        // save
        try {
            userService.saveWithAdminLimit(newUser, false);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("newUser", formUser);
            injectUser(model);
            return "admin/users/create";
        }

        return "redirect:/admin/users/index";
    }

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

        model.addAttribute("editingUser", opt.get());
        injectUser(model);
        return "admin/users/edit";
    }

    @PostMapping("/users/edit/{id}")
    public String updateUser(
            @PathVariable Long id,
            @ModelAttribute("editingUser") User userForm,
            BindingResult result,
            @RequestParam("avatarFile") MultipartFile avatarFile,
            @RequestParam String role,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        // Kiểm tra quyền
        if (principal == null || !hasAdminRole()) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền!");
            return "redirect:/admin/users/index";
        }

        // Kiểm tra user tồn tại
        User editingUser = userService.findById(id).orElse(null);
        if (editingUser == null) {
            redirectAttributes.addFlashAttribute("error", "Người dùng không tồn tại!");
            return "redirect:/admin/users/index";
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

        String phone = userForm.getPhone() != null ? userForm.getPhone().trim() : null;

        if (phone != null) {
            phone = phone.trim();
        }

        // Validate định dạng
        if (phone != null && !phone.matches("^\\d{10}$")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại phải gồm 10 chữ số!");
            redirectAttributes.addFlashAttribute("editingUser", userForm);
            return "redirect:/admin/users/edit/" + id;
        }

        if (phone != null && !phone.equals(editingUser.getPhone())
                && userService.existsByPhone(phone)) {

            redirectAttributes.addFlashAttribute("error", "Số điện thoại đã tồn tại!");
            redirectAttributes.addFlashAttribute("editingUser", userForm);
            return "redirect:/admin/users/edit/" + id;
        }

        // Cập nhật thông tin
        editingUser.setName(userForm.getName().trim());
        editingUser.setPhone(phone);
        editingUser.setEmail(emailLower);
        editingUser.setRole(User.Role.valueOf(role));
        editingUser.syncAdminFromRole();

        // update avatar
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String contentType = avatarFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "File upload phải là hình ảnh!");
                    redirectAttributes.addFlashAttribute("editingUser", userForm);
                    return "redirect:/admin/users/edit/" + id;
                }

                String uploadDir = WebConfig.UPLOAD_ROOT + "avatars/";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String originalName = avatarFile.getOriginalFilename();
                String fileName = generateUniqueFileName(uploadDir, originalName);

                File saveFile = new File(dir, fileName);
                avatarFile.transferTo(saveFile);

                // (optional) xóa ảnh cũ nếu không phải default
                if (editingUser.getAvatar() != null && !editingUser.getAvatar().equals("default.jpg")) {
                    File old = new File(uploadDir + editingUser.getAvatar());
                    if (old.exists()) old.delete();
                }

                editingUser.setAvatar(fileName);

            } catch (Exception e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "Upload avatar thất bại!");
                redirectAttributes.addFlashAttribute("editingUser", userForm);
                return "redirect:/admin/users/edit/" + id;
            }
        }

        // lưu
        try {
            userService.saveWithAdminLimit(editingUser, true);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("editingUser", userForm);
            return "redirect:/admin/users/edit/" + id;
        }

        redirectAttributes.addFlashAttribute("success", "Cập nhật thành công!");

        return "redirect:/admin/users/index";
    }

    // khóa account người dùng
    @PostMapping("/users/lock/{id}")
    public String lockUser(@PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            boolean wasEnabled = user.isEnabled();

            userService.toggleLockUser(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    wasEnabled
                            ? "🔒 Khóa tài khoản thành công!"
                            : "🔓 Mở khóa tài khoản thành công!"
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Có lỗi xảy ra khi cập nhật tài khoản!"
            );
        }

        return "redirect:/admin/users/index";
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

    //ten anh khi upload
    private String generateUniqueFileName(String uploadDir, String originalFileName) {
        File file = new File(uploadDir + originalFileName);
        if (!file.exists()) {
            return originalFileName;
        }

        String name = originalFileName;
        String baseName = name;
        String extension = "";

        int dotIndex = name.lastIndexOf(".");
        if (dotIndex != -1) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex);
        }

        int count = 1;
        String newName;
        do {
            newName = baseName + "(" + count + ")" + extension;
            file = new File(uploadDir + newName);
            count++;
        } while (file.exists());

        return newName;
    }

    // Tải template
    @GetMapping("/users/template")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> downloadTemplate() {

        String header = "name,phone,email,password,role\n"
                + "Nguyen Van A,0912345678,a@gmail.com,123456,EMPLOYEE\n";

        byte[] content = header.getBytes();

        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=user_template.csv")
                .header("Content-Type", "text/csv")
                .body(content);
    }

    // Upload file csv
    @PostMapping("/users/import")
    public String importUsers(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (!hasAdminRole()) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền!");
            return "redirect:/admin/users/index";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file CSV!");
            return "redirect:/admin/users/index";
        }

        // ❗ CHECK ĐỊNH DẠNG FILE
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            redirectAttributes.addFlashAttribute("error",
                    "Chỉ được upload file định dạng .csv!");
            return "redirect:/admin/users/index";
        }

        try {
            List<String> errors = userService.importUsersFromCsv(file);

            if (!errors.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "Import thất bại do dữ liệu không hợp lệ:\n" + String.join("<br>", errors));
            } else {
                redirectAttributes.addFlashAttribute("success",
                        "Import người dùng thành công!");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi đọc file CSV!");
        }

        return "redirect:/admin/users/index";
    }


    // ---------- REDIRECT ROOT ----------
    @GetMapping
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }

}
