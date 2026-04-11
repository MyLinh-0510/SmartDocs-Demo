package edu.uni.smartdocs.controllers.admin;

import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.CategoryRepository;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.FileTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AccessAuthorController {

    private final DocumentRepository documentRepository;
    private final FileTypeRepository fileTypeRepository;
    private final CategoryRepository categoryRepository;

    // Hiển thị danh sách tài liệu để phân quyền
    @GetMapping({"/admin/accessauthor", "/admin/accessauthor/index"})
    public String index(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) Long fileTypeId,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) User.Role role,
                        Model model) {

        Page<Document> pageDocs = documentRepository.filterForAccessAuthor(
                fileTypeId,
                categoryId,
                role,
                PageRequest.of(page, size, Sort.by("id").descending())
        );

        model.addAttribute("documents", pageDocs.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageDocs.getTotalPages());
        model.addAttribute("size", size);

        // giữ lại giá trị đã chọn
        model.addAttribute("selectedFileTypeId", fileTypeId);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedRole", role);

        model.addAttribute("fileTypes", fileTypeRepository.findAll());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("roles",List.of(User.Role.EMPLOYEE, User.Role.CEO));

        return "admin/accessauthor/index";
    }


    // Cập nhật quyền xem tài liệu
    @PostMapping("/admin/accessauthor/update")
    @Transactional
    public String updateRoles(
            @RequestParam Long docId,
            @RequestParam(required = false) List<String> roles,

            @RequestParam(required = false) Long fileTypeId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes
    ) {

        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        doc.getVisibleToRoles().clear();

        if (roles != null && !roles.isEmpty()) {
            roles.stream()
                    .map(User.Role::valueOf)
                    .forEach(doc.getVisibleToRoles()::add);
        }

        redirectAttributes.addFlashAttribute("success", "Cập nhật phân quyền thành công");

        String redirect = "redirect:/admin/accessauthor/index?page=" + page + "&size=" + size;

        if (fileTypeId != null) redirect += "&fileTypeId=" + fileTypeId;
        if (categoryId != null) redirect += "&categoryId=" + categoryId;
        if (role != null && !role.isBlank()) redirect += "&role=" + role;

        return redirect;
    }

}
