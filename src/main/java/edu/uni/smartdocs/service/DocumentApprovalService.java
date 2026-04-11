package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.DocumentStatus;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DocumentApprovalService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Value("${app.base-url}")
    private String baseUrl;

    /* ================= GENERATE SỐ VB ================= */
    private String generateSoVanBan() {
        int year = LocalDate.now().getYear();
        Long maxNumber = documentRepository.findMaxSoVBNumberByYear(year);
        long nextNumber = (maxNumber == null) ? 1 : maxNumber + 1;
        return String.format("%03d/%d/VB-CTY", nextNumber, year);
    }

    /* ================= CEO DUYỆT ================= */
    @Transactional
    public void approve(Long documentId, User ceo, String note) {

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy văn bản"));

        if (doc.getApprover() == null ||
                !doc.getApprover().getId().equals(ceo.getId())) {
            throw new RuntimeException("Bạn không có quyền duyệt tài liệu này");
        }

        if (doc.getStatus() != DocumentStatus.PENDING_APPROVAL) return;

        if (doc.getSoVB() == null) doc.setSoVB(generateSoVanBan());

        doc.setStatus(DocumentStatus.APPROVED);
        doc.setApprovedAt(LocalDateTime.now());
        doc.setApprovalNote(note);
        documentRepository.save(doc);

        User employee = doc.getCreatedBy();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        /* GỬI MAIL */
                        emailService.send(
                                employee.getEmail(),
                                "[SmartDocs] Văn bản đã được phê duyệt",
                                """
                                Kính gửi %s,

                                Văn bản của bạn đã được phê duyệt thành công.

                                ───────────────────────────
                                📄 %s
                                ✍ CEO: %s
                                🕒 %s
                                🧾 Số VB: %s
                                📝 Ghi chú: %s
                                """.formatted(
                                        employee.getFullName(),
                                        doc.getTitle(),
                                        ceo.getFullName(),
                                        doc.getApprovedAt(),
                                        doc.getSoVB(),
                                        (note == null || note.isBlank()) ? "Không có ghi chú" : note
                                )
                        );

                        /* ✔ CHỈ gửi cho nhân viên */
                        notificationService.create(
                                employee,
                                "✅ Văn bản đã được duyệt: " + doc.getTitle(),
                                "/user/documentsu/my-documents"
                        );
                    }
                }
        );
    }

    /* ================= CEO TỪ CHỐI ================= */
    @Transactional
    public void reject(Long documentId, User ceo, String note) {

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy văn bản"));

        if (doc.getApprover() == null ||
                !doc.getApprover().getId().equals(ceo.getId())) {
            throw new RuntimeException("Bạn không có quyền từ chối tài liệu này");
        }

        if (doc.getStatus() != DocumentStatus.PENDING_APPROVAL) return;

        doc.setStatus(DocumentStatus.REJECTED);
        doc.setApprovedAt(LocalDateTime.now());
        doc.setApprovalNote(note);
        documentRepository.save(doc);

        User employee = doc.getCreatedBy();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        /* GỬI MAIL */
                        emailService.send(
                                employee.getEmail(),
                                "[SmartDocs] Văn bản bị từ chối",
                                """
                                Kính gửi %s,

                                Văn bản của bạn đã bị từ chối.

                                📄 %s
                                👤 CEO: %s
                                📝 Lý do: %s
                                """.formatted(
                                        employee.getFullName(),
                                        doc.getTitle(),
                                        ceo.getFullName(),
                                        (note == null || note.isBlank()) ? "Không có ghi chú" : note
                                )
                        );

                        /* ✔ CHỈ gửi cho nhân viên */
                        notificationService.create(
                                employee,
                                "❌ Văn bản bị từ chối: " + doc.getTitle(),
                                "/user/documentsu/my-documents"
                        );
                    }
                }
        );
    }

    /* ================= NHÂN VIÊN TRÌNH KÝ ================= */
    @Transactional
    public void submitForApproval(Long documentId, Long approverId, User employee) {

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy văn bản"));

        if (!doc.getCreatedBy().getId().equals(employee.getId())) {
            throw new RuntimeException("Bạn không có quyền trình ký văn bản này");
        }

        if (doc.getStatus() != DocumentStatus.DRAFT) {
            throw new RuntimeException("Văn bản không ở trạng thái nháp");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt"));

        doc.setApprover(approver);
        doc.setStatus(DocumentStatus.PENDING_APPROVAL);
        doc.setSubmittedAt(LocalDateTime.now());
        documentRepository.save(doc);

        String reviewLink = baseUrl + "/user/documentsu/my-documents";

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        /* GỬI MAIL THÔNG BÁO CHO CEO */
                        emailService.send(
                                approver.getEmail(),
                                "[SmartDocs] Yêu cầu ký duyệt văn bản",
                                """
                                Kính gửi %s,

                                Có một văn bản mới cần bạn duyệt:

                                📄 %s
                                👤 Người gửi: %s
                                🕒 %s

                                👉 Xem tại: %s
                                """.formatted(
                                        approver.getFullName(),
                                        doc.getTitle(),
                                        employee.getFullName(),
                                        doc.getSubmittedAt(),
                                        reviewLink
                                )
                        );

                        /* ✔ CHỈ CEO nhận thông báo */
                        notificationService.create(
                                approver,
                                "📨 Bạn có văn bản cần duyệt: " + doc.getTitle(),
                                "/user/documentsu/my-documents"
                        );

                        /* ✔ CHỈ người trình nhận thông báo */
                        notificationService.create(
                                employee,
                                "📤 Bạn đã trình ký văn bản: " + doc.getTitle(),
                                "/user/documentsu/my-documents"
                        );
                    }
                }
        );
    }
}