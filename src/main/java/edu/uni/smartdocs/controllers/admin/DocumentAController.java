package edu.uni.smartdocs.controllers.admin;

import edu.uni.smartdocs.config.WebConfig;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.UserRepository;
import edu.uni.smartdocs.service.CategoryService;
import edu.uni.smartdocs.service.DocumentService;
import edu.uni.smartdocs.service.FileConvertService;
import edu.uni.smartdocs.service.FileTypeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DocumentAController {

    private final DocumentService documentService;
    private final FileTypeService fileTypeService;
    private final CategoryService categoryService;

    private final FileConvertService fileConvertService;


    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    // ========================= DANH SÁCH =========================
    @GetMapping("/documents/index")
    public String listDocuments(Model model, @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            model.addAttribute("documents", List.of());
            return "admin/documents/index";
        }

        String email = principal.getUsername();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            model.addAttribute("documents", List.of());
            return "admin/documents/index";
        }

        List<Document> allDocs = documentRepository.findAll();
        List<Document> visibleDocs;

        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.CEO) {
            visibleDocs = allDocs;
        } else {
            visibleDocs = allDocs.stream()
                    .filter(doc -> doc.getIsVisible() != null && doc.getIsVisible())
                    .toList();
        }

        model.addAttribute("documents", visibleDocs);
        return "admin/documents/index";
    }

    // ========================= FORM TẠO =========================
    @GetMapping("/documents/create")
    public String showCreateForm(Model model) {
        model.addAttribute("fileTypes", fileTypeService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/documents/create";
    }

    // ========================= XỬ LÝ TẠO =========================
    @PostMapping("/documents/create")
    public String createDocument(@RequestParam("title") String title,
                                 @RequestParam("description") String description,
                                 @RequestParam("fileTypeId") Long fileTypeId,
                                 @RequestParam("categoryId") Long categoryId,
                                 @RequestParam(value = "meta", required = false) String meta,
                                 @RequestParam(value = "isVisible", defaultValue = "false") boolean isVisible,
                                 @RequestParam("file") MultipartFile file,
                                 @AuthenticationPrincipal UserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User creator = userRepository.findByEmail(principal.getUsername()).orElse(null);
            documentService.saveDocument(title, description, fileTypeId, categoryId, meta, isVisible, file, creator);
            redirectAttributes.addFlashAttribute("success", "Tạo tài liệu thành công!");
            return "redirect:/admin/documents/index";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tải file: " + e.getMessage());
            return "redirect:/admin/documents/create";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu tài liệu!");
            return "redirect:/admin/documents/create";
        }
    }

    // ========================= UPLOAD FILE =========================
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws Exception {

        String originalDir = WebConfig.UPLOAD_ROOT + "original/";
        String pdfDir = WebConfig.UPLOAD_ROOT + "pdf/";

        // Tạo thư mục nếu chưa có
        Files.createDirectories(Paths.get(originalDir));
        Files.createDirectories(Paths.get(pdfDir));

        // 1. Lưu file gốc
        String originalPath = originalDir + file.getOriginalFilename();
        file.transferTo(new File(originalPath));

        // 2. Convert sang PDF (LibreOffice/JODConverter)
        fileConvertService.convertToPdf(originalPath, pdfDir);

        return "redirect:/success";
    }

    // ========================= CHI TIẾT =========================
    @GetMapping("/documents/detail/{id}")
    public String detail(@PathVariable Long id, Model model, HttpServletRequest request) {
        Document doc = documentService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));
        model.addAttribute("doc", doc);

        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();

        String encodedFileName = URLEncoder.encode(doc.getFilename(), StandardCharsets.UTF_8);

        model.addAttribute("publicBaseUrl", baseUrl);
        model.addAttribute("encodedFilename", encodedFileName);
        return "admin/documents/detail";
    }

    // ========================= XOÁ =========================
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

    // ========================= XEM FILE =========================
    @GetMapping("/documents/view/{id}")
    public String viewDocument(@PathVariable Long id, Model model, HttpServletRequest request) {

        Document doc = documentService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        // Lấy base URL
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();

        String encodedFileName = doc.getFilename();

        model.addAttribute("doc", doc);
        model.addAttribute("publicBaseUrl", baseUrl);
        model.addAttribute("encodedFilename", encodedFileName);

        return "admin/documents/detail";
    }


    // ========================= FORM SỬA =========================
    @GetMapping("/documents/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Document document = documentService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        model.addAttribute("document", document);
        model.addAttribute("fileTypes", fileTypeService.findAll());
        model.addAttribute("categories", categoryService.findAll());

        return "admin/documents/edit"; // file edit.html
    }

    // ========================= XỬ LÝ SỬA =========================
    @PostMapping("/documents/edit/{id}")
    public String updateDocument(@PathVariable Long id,
                                 @RequestParam("title") String title,
                                 @RequestParam("description") String description,
                                 @RequestParam("fileTypeId") Long fileTypeId,
                                 @RequestParam("categoryId") Long categoryId,
                                 @RequestParam("meta") String meta,
                                 @RequestParam(value = "isVisible", defaultValue = "false") boolean isVisible,
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal UserDetails principal) {

        try {
            User editor = userRepository.findByEmail(principal.getUsername()).orElse(null);

            documentService.updateDocument(id, title, description, fileTypeId,
                    categoryId, meta, isVisible, file, editor);

            redirectAttributes.addFlashAttribute("success", "Cập nhật thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật tài liệu: " + e.getMessage());
        }

        return "redirect:/admin/documents/index";
    }



}
