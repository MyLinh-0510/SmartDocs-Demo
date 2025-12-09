package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DocumentService documentService;

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        //Nếu chưa đăng nhập → hiển thị trang chủ public
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("latestDocs", documentService.getLatestVisibleDocuments());
            return "user/home"; // Trang chủ public có danh sách tài liệu mới nhất
        }

        //Nếu là ADMIN → chuyển vào Dashboard
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        // 🔸 Nếu là CEO hoặc EMPLOYEE → hiển thị site/home với tài liệu mới
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CEO")) ||
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))) {

            model.addAttribute("latestDocs", documentService.getLatestVisibleDocuments());
            return "user/home";
        }

        // 🔸 Nếu không có role hợp lệ → quay lại login
        return "redirect:/account/login";
    }
}
