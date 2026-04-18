package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.dto.DocumentSearchDTO;
import edu.uni.smartdocs.models.*;
import edu.uni.smartdocs.repository.CategoryRepository;
import edu.uni.smartdocs.repository.FileTypeRepository;
import edu.uni.smartdocs.repository.UserRepository;
import edu.uni.smartdocs.security.CustomUserDetails;
import edu.uni.smartdocs.service.*;
import edu.uni.smartdocs.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class DocumentUController {

    private final DocumentService documentService;
    private final UserDocumentActionService userDocumentActionService;
    private final UserService userService;
    private final DocumentRepository documentRepository;
    private final LogDownloadService logdownloadService;
    private final UserRepository userRepository;
    private final DocumentApprovalService approvalService;
    private final FileTypeRepository fileTypeRepository;
    private final CategoryRepository categoryRepository;

    /* search api */
    @GetMapping("/search-api")
    @ResponseBody
    public List<DocumentSearchDTO> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("Chưa đăng nhập");
            return List.of();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        System.out.println("User: " + user.getEmail() + " | Role: " + user.getRole());

        return documentService.searchDocuments(keyword, categoryId, user);
    }

    /* download file */
    @GetMapping("/documents/download/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        Document doc = documentService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        checkImportantContractPermission(user, doc);

        Path filePath = Paths.get("uploads/original").resolve(doc.getFilename());

        try {
            logdownloadService.logDownload(user, id);

            Resource resource = new UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            String encodedFileName = URLEncoder
                    .encode(doc.getFilename(), StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName
                    )
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /*Xem file*/
    @GetMapping("/documents/view/{meta}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public ResponseEntity<Resource> viewPdf(@PathVariable String meta) {

        Document doc = documentService.findByMeta(meta);

        Path filePath = Paths.get("uploads/pdf").resolve(doc.getPdfFilename());

        try {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Không đọc được file PDF");
        }
    }

    /* pdf preview + log */
    @GetMapping("/pdf-preview/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public ResponseEntity<Resource> previewPdf(
            @PathVariable Long id,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        Document doc = documentService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        // Chặn hợp dồng quan trọng
        checkImportantContractPermission(user, doc);

        userDocumentActionService.logViewed(user, id);

        Path imagePath = Paths.get("uploads/previews/" + id + "/page-1.png");

        try {
            Resource resource = new UrlResource(imagePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /* my documents (CEO + EMPLOYEE) */
    @GetMapping("/documentsu/my-documents")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public String myDocuments(
            @RequestParam(required = false) DocumentStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        User currentUser = userDetails.getUser();
        boolean isCEO = currentUser.getRole() == User.Role.CEO;

        List<Document> documents;
        if (isCEO) {
            documents = (status == null)
                    ? documentRepository.findByApproverOrderByCreatedAtDesc(currentUser)
                    : documentRepository.findByApproverAndStatusOrderByCreatedAtDesc(currentUser, status);
        } else {
            documents = (status == null)
                    ? documentRepository.findByCreatedByOrderByCreatedAtDesc(currentUser)
                    : documentRepository.findByCreatedByAndStatusOrderByCreatedAtDesc(currentUser, status);
        }

        model.addAttribute("documents", documents);
        model.addAttribute("isCEO", isCEO);
        model.addAttribute("currentStatus", status);
        model.addAttribute("ceoList", userRepository.findByRole(User.Role.CEO));

        return "user/documentsu/my-documents";
    }

    // TOP 5 mới nhất (visible + role phù hợp)
    @GetMapping("/latest-api")
    @ResponseBody
    public List<DocumentSearchDTO> getLatest(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) return List.of();

        User user = userDetails.getUser();

        return documentService.getLatestVisibleDocuments().stream()
                .filter(d -> d.getVisibleToRoles().contains(user.getRole()))
                .limit(5)
                .map(d -> new DocumentSearchDTO(
                        d.getId(),
                        d.getTitle(),
                        d.getCategory() != null ? d.getCategory().getName() : "",
                        d.getPdfFilename(),
                        d.getMeta()
                ))
                .toList();
    }

    // Tài liệu tải nhiều nhất (top 5 download)
    @GetMapping("/popular-api")
    @ResponseBody
    public List<DocumentSearchDTO> getPopular(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) return List.of();

        User user = userDetails.getUser();

        return documentService.getPopularDocumentsWithActions(5, user.getId());
    }

    // Tài liệu yêu thích / lưu / ghim (dựa trên currentSavedType)
    @GetMapping("/saved-api")
    @ResponseBody
    public List<DocumentSearchDTO> getSaved(
            @RequestParam String type,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) return List.of();

        User user = userDetails.getUser();

        UserDocumentAction.ActionType actionType = switch (type) {
            case "FAVORITE" -> UserDocumentAction.ActionType.FAVORITE;
            case "SAVED" -> UserDocumentAction.ActionType.SAVED;
            case "PINNED" -> UserDocumentAction.ActionType.PINNED;
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        };

        return documentService.getDocumentsByAction(user.getId(), actionType);
    }

    // Tài liệu liên quan / lịch sử xem
    @GetMapping("/history-api")
    @ResponseBody
    public List<DocumentSearchDTO> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) return List.of();

        User user = userDetails.getUser();

        // Ví dụ: top 5 viewed gần nhất
        return documentService.getRecentViewed(user.getId()).stream()
                .limit(5)
                .toList();
    }

    /* UPLOAD DOCUMENT (EMPLOYEE) */
    @PostMapping("/documentsu/upload")
    public String uploadMultipleFiles(
            @RequestParam("titles") List<String> titles,
            @RequestParam("files") MultipartFile[] files,
            @AuthenticationPrincipal(expression = "username") String email,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User employee = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            if (employee.getRole() == User.Role.CEO) {
                throw new RuntimeException("CEO không được upload tài liệu");
            }

            Long fileTypeId = fileTypeRepository.findAll()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Chưa có FileType"))
                    .getId();

            Long categoryId = categoryRepository.findByName("Văn bản chưa duyệt")
                    .orElseThrow(() -> new RuntimeException("Thiếu category Văn bản chưa duyệt"))
                    .getId();

            for (int i = 0; i < titles.size(); i++) {

                MultipartFile file = files[i];

                if (!file.isEmpty()) {

                    documentService.saveDocument(
                            titles.get(i),
                            null,
                            fileTypeId,
                            categoryId,
                            null,
                            false,
                            file,
                            employee
                    );
                }
            }

            redirectAttributes.addFlashAttribute(
                    "success",
                    "📄 Upload thành công tài liệu!"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Upload thất bại!"
            );
        }

        return "redirect:/user/documentsu/my-documents";
    }

    /* SUBMIT FOR APPROVAL (EMPLOYEE)*/
    @PostMapping("/documentsu/submit-approval")
    public String submitApproval(
            @RequestParam Long documentId,
            @RequestParam Long approverId,
            @AuthenticationPrincipal(expression = "username") String email,
            RedirectAttributes redirectAttributes
    ) {
        User employee = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        approvalService.submitForApproval(documentId, approverId, employee);

        redirectAttributes.addFlashAttribute(
                "success",
                "📨 Văn bản đã được gửi CEO ký duyệt!"
        );

        return "redirect:/user/documentsu/my-documents";
    }

    /* CEO APPROVE DOCUMENT */
    @PreAuthorize("hasRole('CEO')")
    @PostMapping("/documentsu/approve")
    public String approveDocument(
            @RequestParam Long documentId,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal(expression = "username") String email,
            RedirectAttributes redirectAttributes
    ) {
        User ceo = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        approvalService.approve(documentId, ceo, note); // truyền note

        redirectAttributes.addFlashAttribute(
                "success",
                "✅ Văn bản đã được duyệt!"
        );

        return "redirect:/user/documentsu/my-documents";
    }

    /* CEO REJECT DOCUMENT */
    @PreAuthorize("hasRole('CEO')")
    @PostMapping("/documentsu/reject")
    public String rejectDocument(
            @RequestParam Long documentId,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal(expression = "username") String email,
            RedirectAttributes redirectAttributes
    ) {
        User ceo = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        approvalService.reject(documentId, ceo, note);

        redirectAttributes.addFlashAttribute(
                "error",
                "❌ Văn bản đã bị từ chối!"
        );

        return "redirect:/user/documentsu/my-documents";
    }

    /* Cho phép CEO truy cập vào tài liệu này  */
    private void checkImportantContractPermission(User user, Document doc) {
        if (doc.getCategory() != null &&
                "Hợp đồng quan trọng".equalsIgnoreCase(doc.getCategory().getName()) &&
                user.getRole() != User.Role.CEO) {
            throw new RuntimeException("Bạn không có quyền truy cập tài liệu này");
        }
    }

    // Chat nhân viên với nhân viên
    @PostMapping("/api/upload")
    @ResponseBody
    public Map<String, String> uploadFile(@RequestParam("file") MultipartFile file) throws java.io.IOException {

        // Tạo tên file unique
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // Lưu vào thư mục uploads/
        Path path = Paths.get("uploads/" + fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", "/uploads/" + fileName);      // URL để truy cập sau
        response.put("fileName", file.getOriginalFilename());

        return response;
    }


}