package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.models.Category;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.CategoryService;
import edu.uni.smartdocs.service.DocumentService;
import edu.uni.smartdocs.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;


@Controller
@RequestMapping("/user")
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
    }

    //TRANG CHỦ
    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String home(Model model) {
        List<Document> docs = documentService.findVisibleDocuments();
        if (docs == null) docs = new ArrayList<>();
        model.addAttribute("latestDocs", docs);
        model.addAttribute("totalDocs", docs.size());
        model.addAttribute("totalUsers", userService.countUsers());
        return "user/home";
    }

    //TRANG TÌM KIẾM
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String search(@RequestParam(required = false) String keyword, Model model) {
        List<Document> docs = (keyword == null || keyword.isBlank())
                ? documentService.findVisibleDocuments()
                : documentService.findByKeyword(keyword);

        model.addAttribute("keyword", keyword);
        model.addAttribute("results", docs);
        return "user/search";
    }

    // 📄 CHI TIẾT TÀI LIỆU
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String detail(@PathVariable Long id, Model model) {
        Document document = documentService.findById(id).orElse(null);
        model.addAttribute("document", document);
        return "user/documentsu/detail";
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

        return "user/documentsu/documents-user";
    }


}
