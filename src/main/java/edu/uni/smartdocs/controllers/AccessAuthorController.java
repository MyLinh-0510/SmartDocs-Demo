package edu.uni.smartdocs.controllers;

import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.CategoryRepository;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.FileTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AccessAuthorController {

    private final DocumentRepository documentRepository;
    private final FileTypeRepository fileTypeRepository;
    private final CategoryRepository categoryRepository;

    // Hiển thị danh sách
    @GetMapping({"/admin/accessauthor", "/admin/accessauthor/index"})
    public String index(@RequestParam(required = false) Long fileTypeId,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) String role,
                        Model model) {

        List<Document> docs = documentRepository.findAll();

        if (fileTypeId != null) {
            docs = docs.stream()
                    .filter(d -> d.getFileType() != null && d.getFileType().getId().equals(fileTypeId))
                    .toList();
        }

        if (categoryId != null) {
            docs = docs.stream()
                    .filter(d -> d.getCategory() != null && d.getCategory().getId().equals(categoryId))
                    .toList();
        }

        if (role != null && !role.isBlank()) {
            try {
                User.Role roleEnum = User.Role.valueOf(role);
                docs = docs.stream()
                        .filter(d -> d.getVisibleToRoles().contains(roleEnum))
                        .toList();
            } catch (IllegalArgumentException ignored) {}
        }

        model.addAttribute("documents", docs);
        model.addAttribute("fileTypes", fileTypeRepository.findAll());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("roles", User.Role.values());

        model.addAttribute("selectedFileTypeId", fileTypeId);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedRole", role);

        return "admin/accessauthor/index";
    }

    // Cập nhật quyền
    @PostMapping("/admin/accessauthor/update")
    public String updateRoles(@RequestParam Long docId,
                              @RequestParam(required = false) List<String> roles) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        Set<User.Role> selectedRoles = (roles == null)
                ? Set.of()
                : roles.stream().map(User.Role::valueOf).collect(Collectors.toSet());

        doc.setVisibleToRoles(selectedRoles);
        documentRepository.save(doc);

        return "redirect:/admin/accessauthor";
    }
}
