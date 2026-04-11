package edu.uni.smartdocs.controllers.admin;

import edu.uni.smartdocs.config.WebConfig;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.DocumentStatus;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.CategoryRepository;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.FileTypeRepository;
import edu.uni.smartdocs.repository.UserRepository;
import edu.uni.smartdocs.security.CustomUserDetails;
import edu.uni.smartdocs.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DocumentAController {

    private final DocumentService documentService;
    private final FileTypeService fileTypeService;
    private final CategoryService categoryService;
    private final UserDocumentActionService actionService;


    private final FileConvertService fileConvertService;


    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FileTypeRepository fileTypeRepository;

    // list tài liệu
    @GetMapping("/documents/index")
    public String listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long fileTypeId,
            @RequestParam(required = false) DocumentStatus status,
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) Integer fail,
            Model model
    ) {

        User admin = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));


        Page<Document> documentPage =
                documentService.getDocumentsForAdmin(
                        admin,
                        page,
                        size,
                        categoryId,
                        fileTypeId,
                        status
                );

        model.addAttribute("documents", documentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", documentPage.getTotalPages());
        model.addAttribute("size", size);

        // Load dữ liệu cho dropdown
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("fileTypes", fileTypeService.findAll());

        // giữ trạng thái lọc
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("fileTypeId", fileTypeId);
        model.addAttribute("status", status);

        // thông báo upload
        if(success != null){
            String msg = "Bạn đã upload thành công " + success + " tài liệu";

            if(fail != null && fail > 0){
                msg += " (lỗi " + fail + " tài liệu)";
            }
            msg += ".";
            model.addAttribute("multiMessage", msg);
        }

        return "admin/documents/index";
    }

    // tạo tài liệu
    @GetMapping("/documents/create")
    public String showCreateForm(Model model) {
        model.addAttribute("fileTypes", fileTypeService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/documents/create";
    }

    // xử lý tạo tài liệu
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
        if (file.getSize() > 10 * 1024 * 1024) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "❌ File vượt quá 10MB, vui lòng chọn file nhỏ hơn"
            );
            return "redirect:/admin/documents/create";
        }

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

    // Upload nhiều file cùng lúc
    @GetMapping("/documents/create-multiple")
    public String showUploadMultiplePage(Model model) {

        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("fileTypes", fileTypeService.findAll());

        return "admin/documents/create-multiple";
    }

    @PostMapping("/documents/create-multiple")
    @ResponseBody
    public Map<String, Object> uploadMultiple(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("titles") String[] titles,
            @RequestParam("descriptions") String[] descriptions,
            @RequestParam("fileTypeIds") String[] fileTypeIds,
            @RequestParam("categoryIds") String[] categoryIds,
            @RequestParam("isVisibles") String[] isVisibles,
            @AuthenticationPrincipal UserDetails principal
    ) {

        int success = 0;
        int fail = 0;

        List<Map<String, String>> errors = new ArrayList<>();

        User creator = userRepository
                .findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        for (int i = 0; i < files.length; i++) {

            MultipartFile file = files[i];

            try {

                if (file == null || file.isEmpty()) {
                    throw new RuntimeException("File rỗng");
                }

                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new RuntimeException("File > 10MB");
                }

                // ✅ chống lệch index
                String title = i < titles.length ? titles[i] : file.getOriginalFilename();
                String desc = i < descriptions.length ? descriptions[i] : null;

                Long typeId = i < fileTypeIds.length && !fileTypeIds[i].isEmpty()
                        ? Long.parseLong(fileTypeIds[i]) : null;

                Long cateId = i < categoryIds.length && !categoryIds[i].isEmpty()
                        ? Long.parseLong(categoryIds[i]) : null;

                Boolean visible = i < isVisibles.length
                        ? Boolean.parseBoolean(isVisibles[i])
                        : true;

                if (typeId == null) throw new RuntimeException("Chưa chọn loại file");
                if (cateId == null) throw new RuntimeException("Chưa chọn danh mục");

                documentService.saveDocument(
                        title,
                        desc,
                        typeId,
                        cateId,
                        null,
                        visible,
                        file,
                        creator
                );

                success++;

            } catch (Exception e) {

                fail++;

                errors.add(Map.of(
                        "index", String.valueOf(i),
                        "message", e.getMessage()
                ));
            }
        }

        return Map.of(
                "success", success,
                "fail", fail,
                "errors", errors
        );
    }

    // upload file
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

    // trang tài liệu chi tiết
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


    // form sửa
    @GetMapping("/documents/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Document document = documentService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        model.addAttribute("document", document);
        model.addAttribute("fileTypes", fileTypeService.findAll());
        model.addAttribute("categories", categoryService.findAll());

        return "admin/documents/edit"; // file edit.html
    }

    // xử lý suwra
    @PostMapping("/documents/edit/{id}")
    public String updateDocument(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("fileTypeId") Long fileTypeId,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "meta", required = false, defaultValue = "") String meta,
            @RequestParam(value = "isVisible", defaultValue = "false") boolean isVisible,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes) {

        try {
            // Lấy user đang đăng nhập
            User editor = userRepository.findByEmail(principal.getUsername())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng đăng nhập"));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println(auth);


            documentService.updateDocument(id, title, description, fileTypeId, categoryId, meta, isVisible, file, editor);

            redirectAttributes.addFlashAttribute("success", "Cập nhật tài liệu thành công!");
            return "redirect:/admin/documents/index";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật: " + e.getMessage());
            return "redirect:/admin/documents/edit/" + id;
        }
    }

    @GetMapping("/user/pdf-preview/{docId}")
    @ResponseBody
    public byte[] preview(@PathVariable Long docId) throws IOException {
        Path path = Paths.get("uploads/previews/" + docId + "/page-1.png");
        return Files.readAllBytes(path);
    }

}
