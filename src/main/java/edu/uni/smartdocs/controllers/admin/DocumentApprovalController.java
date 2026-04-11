package edu.uni.smartdocs.controllers.admin;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.DocumentStatus;
import edu.uni.smartdocs.repository.CategoryRepository;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.FileTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/document-approvals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DocumentApprovalController {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final FileTypeRepository fileTypeRepository;

    /* LIST */
    @GetMapping("/index")
    public String index(
            @RequestParam(defaultValue = "0") int pendingPage,
            @RequestParam(defaultValue = "0") int processedPage,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {

        Pageable pendingPageable = PageRequest.of(
                pendingPage, size,
                Sort.by("submittedAt").descending()
        );

        Pageable processedPageable = PageRequest.of(
                processedPage, size,
                Sort.by("approvedAt").descending()
        );

        // 1️⃣ Bảng chưa duyệt
        Page<Document> pendingDocuments =
                documentRepository.findByStatusAndApproverIsNotNull(
                        DocumentStatus.PENDING_APPROVAL,
                        pendingPageable
                );

        // 2️⃣ Bảng đã xử lý (APPROVED + REJECTED)
        Page<Document> processedDocuments =
                documentRepository.findByStatusInAndApproverIsNotNull(
                        List.of(DocumentStatus.APPROVED, DocumentStatus.REJECTED),
                        processedPageable
                );

        // Init lazy
        pendingDocuments.getContent().forEach(doc -> {
            Hibernate.initialize(doc.getCreatedBy());
            Hibernate.initialize(doc.getApprover());
        });

        processedDocuments.getContent().forEach(doc -> {
            Hibernate.initialize(doc.getCreatedBy());
            Hibernate.initialize(doc.getApprover());
        });

        model.addAttribute("pendingDocuments", pendingDocuments.getContent());
        model.addAttribute("processedDocuments", processedDocuments.getContent());

        model.addAttribute("pendingCurrentPage", pendingPage);
        model.addAttribute("pendingTotalPages", pendingDocuments.getTotalPages());

        model.addAttribute("processedCurrentPage", processedPage);
        model.addAttribute("processedTotalPages", processedDocuments.getTotalPages());

        model.addAttribute("size", size);

        return "admin/document-approvals/index";
    }

    /* EDIT */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        model.addAttribute("document", doc);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("fileTypes", fileTypeRepository.findAll());

        return "admin/document-approvals/edit";
    }

    @PostMapping("/edit/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean isVisible,
            RedirectAttributes redirect
    ) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        DocumentStatus status = doc.getStatus();

        switch (status) {
            case PENDING_APPROVAL:
                redirect.addFlashAttribute(
                        "error",
                        "⏳ Tài liệu đang trong quá trình trình ký, không thể thay đổi trạng thái hiển thị"
                );
                return "redirect:/admin/document-approvals/index";

            case REJECTED:
                redirect.addFlashAttribute(
                        "error",
                        "🚫 Tài liệu đã bị từ chối, không thể thay đổi trạng thái hiển thị"
                );
                return "redirect:/admin/document-approvals/index";

            case APPROVED:
                // được phép chỉnh
                doc.setVisible(isVisible);
                documentRepository.save(doc);

                redirect.addFlashAttribute(
                        "success",
                        "✅ Cập nhật trạng thái hiển thị thành công"
                );
                return "redirect:/admin/document-approvals/index";

            default:
                redirect.addFlashAttribute(
                        "error",
                        "❌ Trạng thái tài liệu không hợp lệ"
                );
                return "redirect:/admin/document-approvals/index";
        }
    }

    @PostMapping("/reject")
    public String rejectDocument(@RequestParam Long documentId,
                                 @RequestParam String note,
                                 RedirectAttributes redirect) {

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        if (doc.getStatus() != DocumentStatus.PENDING_APPROVAL) {
            redirect.addFlashAttribute("error", "Chỉ có thể từ chối tài liệu đang chờ duyệt");
            return "redirect:/admin/document-approvals/index";
        }

        doc.setStatus(DocumentStatus.REJECTED);
        doc.setApprovalNote(note);
        doc.setApprovedAt(java.time.LocalDateTime.now());

        documentRepository.save(doc);

        redirect.addFlashAttribute("success", "Đã từ chối tài liệu");
        return "redirect:/admin/document-approvals/index";
    }

    @PostMapping("/toggle-visibility")
    @ResponseBody
    public ResponseEntity<?> toggleVisibility(@RequestParam Long id) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        if (doc.getStatus() != DocumentStatus.APPROVED) {
            return ResponseEntity.badRequest()
                    .body("Chỉ tài liệu đã duyệt mới được thay đổi hiển thị");
        }

        doc.setVisible(!doc.isVisible());
        documentRepository.save(doc);

        return ResponseEntity.ok().build();
    }


}
