package edu.uni.smartdocs.controllers;

import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.service.CategoryService;
import edu.uni.smartdocs.service.DocumentService;
import edu.uni.smartdocs.service.FileTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final FileTypeService fileTypeService;
    private final CategoryService categoryService;

    // DANH SÁCH
    @GetMapping("/documents/index")
    public String listDocuments(Model model) {
        model.addAttribute("documents", documentService.findAll());
        return "admin/documents/index";
    }

    // HIỂN THỊ FORM TẠO
    @GetMapping("/documents/create")
    public String showCreateForm(Model model) {
        model.addAttribute("fileTypes", fileTypeService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/documents/create";
    }

    // XỬ LÝ TẠO
    @PostMapping("/documents/create")
    public String createDocument(@RequestParam("title") String title,
                                 @RequestParam("description") String description,
                                 @RequestParam("fileTypeId") Long fileTypeId,
                                 @RequestParam("categoryId") Long categoryId,
                                 @RequestParam(value = "meta", required = false) String meta,
                                 @RequestParam(value = "isVisible", defaultValue = "false") boolean isVisible,
                                 @RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            documentService.saveDocument(title, description, fileTypeId, categoryId, meta, isVisible, file);
            redirectAttributes.addFlashAttribute("success", "Tạo tài liệu thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tải file: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu tài liệu!");
        }
        return "redirect:/admin/documents/index";
    }

    // CHI TIẾT
    @GetMapping("/documents/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Document doc = documentService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));
        model.addAttribute("doc", doc);
        return "admin/documents/detail";
    }

    // XOÁ
    @PostMapping("/documents/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            documentService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Xoá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xoá tài liệu!");
        }
        return "redirect:/admin/documents/index";
    }
}
