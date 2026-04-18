package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.dto.DocumentSearchDTO;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class SiteController {

    private final DocumentService documentService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ContactService contactService;
    private final NotificationService notificationService;

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute
    public void addCategoriesToAllPages(Model model) {
        model.addAttribute("allCategories", categoryService.findAll());
    }

    // home
    @GetMapping("/home-page")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String homepage(Model model) {

        List<Document> docs = documentService.findVisibleDocuments();

        model.addAttribute("latestDocs", docs);
        model.addAttribute("totalDocs", docs.size());
        model.addAttribute("totalUsers", userService.countUsers());
        model.addAttribute("totalContacts", contactService.count());

        return "/user/home-page";
    }

    // search
    @GetMapping("/documentsu/document-page")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String documentsUser(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            Principal principal,
            Model model
    ) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/user/home-page";
        }

        // === SỬA Ở ĐÂY ===
        if (keyword != null) {
            keyword = keyword.trim();                    // bỏ khoảng trắng thừa
            if (keyword.isEmpty()) {
                keyword = null;                          // nếu chỉ toàn space thì coi như không có keyword
            }
        }

        List<DocumentSearchDTO> documents =
                documentService.searchDocuments(keyword, categoryId, user);

        model.addAttribute("documents", documents);
        model.addAttribute("keyword", keyword);          // truyền keyword đã trim về view
        model.addAttribute("categoryId", categoryId);

        return "/user/documentsu/document-page";
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String notifications(
            Principal principal,
            Model model
    ) {
        if (principal == null) {
            return "redirect:/admin/account/login";
        }

        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/user/home-page";
        }

        model.addAttribute(
                "notifications",
                notificationService.getMyNotifications(user)
        );
        model.addAttribute("pageTitle", "Thông báo");

        // vào trang là mark all read
        notificationService.markAllAsRead(user);

        return "user/notifications";
    }

}
