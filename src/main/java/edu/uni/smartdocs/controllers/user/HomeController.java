package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.dto.DocumentSearchDTO;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.DocumentService;
import edu.uni.smartdocs.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DocumentService documentService;
    private final UserService userService;

    // ROOT
    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/user/home-page";
        }

        if (authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        return "redirect:/user/home-page";
    }

    // API lấy 10 tài liệu mới nhất
    @GetMapping("/user/latest")
    @ResponseBody
    public List<DocumentSearchDTO> getLatestDocsHome(Authentication auth) {

        User user = userService.getCurrentUser(auth);

        List<Document> docs = documentService.getLatestDocuments(user.getRole());

        return docs.stream()
                .limit(10) // 🔥 HOME = 10
                .map(doc -> new DocumentSearchDTO(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getCategory() != null ? doc.getCategory().getName() : "",
                        doc.getPdfFilename()
                ))
                .toList();
    }
}