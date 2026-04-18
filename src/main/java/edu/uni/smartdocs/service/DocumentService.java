package edu.uni.smartdocs.service;

import edu.uni.smartdocs.dto.DocumentSearchDTO;
import edu.uni.smartdocs.models.*;
import edu.uni.smartdocs.repository.*;
import edu.uni.smartdocs.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileTypeRepository fileTypeRepository;
    private final CategoryRepository categoryRepository;
    private final FileConvertService fileConvertService;
    private final UserDocumentActionRepository actionRepo;
    private final PdfPreviewService pdfPreviewService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final LogDownloadService downloadService;

    private static final String UPLOAD_DIR = "uploads/original/";
    private static final String PDF_DIR = "uploads/pdf/";

    /* 10 tài liệu mới nhất */
    public List<Document> getLatestDocuments(User.Role role) {
        return documentRepository.findTop10ForUser(role, PageRequest.of(0, 10));
    }

    /*BASIC*/
    public Optional<Document> findById(Long id) {
        return documentRepository.findById(id);
    }

    public Document findByMeta(String meta) {
        return documentRepository.findByMeta(meta)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));
    }

    public List<Document> getLatestVisibleDocuments() {
        return documentRepository.findTop20ByIsVisibleTrueOrderByCreatedAtDesc();
    }

    public long count() {
        return documentRepository.count();
    }

    /*SEARCH – KHỚP HTML category = category.name*/
    public List<DocumentSearchDTO> searchDocuments(String keyword, Long categoryId, User user) {
        System.out.println("Service called | category: " + categoryId + " | role: " + user.getRole());

        if (keyword != null && keyword.isBlank()) keyword = null;

        List<Document> docs = documentRepository.searchDocuments(keyword, categoryId, user.getRole());
        System.out.println("Repository returned " + docs.size() + " documents");

        return docs.stream()
                .map(d -> {
                    DocumentSearchDTO dto = new DocumentSearchDTO(
                            d.getId(),
                            d.getTitle(),
                            d.getCategory() != null ? d.getCategory().getName() : "",
                            d.getPdfFilename(),
                            d.getMeta()
                    );

                    dto.setMeta(d.getMeta());

                    return dto;
                })
                .toList();
    }

    private User.Role getCurrentUserRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails cud)) {
            return null;
        }

        return cud.getUser().getRole();
    }

    public List<Document> findVisibleDocuments() {
        return documentRepository.findByIsVisibleTrue();
    }

    // Lưu file
    private String storeFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Lấy tên gốc
            String originalName = file.getOriginalFilename();
            if (originalName == null) {
                throw new RuntimeException("Tên file không hợp lệ");
            }

            // Tách tên và phần mở rộng
            String name = originalName;
            String extension = "";

            int dotIndex = originalName.lastIndexOf(".");
            if (dotIndex != -1) {
                name = originalName.substring(0, dotIndex);
                extension = originalName.substring(dotIndex);
            }

            // Loại bỏ ký tự đặc biệt (không phải dấu .)
            name = name.replaceAll("[^a-zA-Z0-9 _-]", "").trim();

            // File path đầu tiên
            Path filePath = uploadPath.resolve(originalName);

            // Nếu file tồn tại → thêm (1), (2)...
            int counter = 1;
            while (Files.exists(filePath)) {
                String newFileName = name + " (" + counter + ")" + extension;
                filePath = uploadPath.resolve(newFileName);
                counter++;
            }

            // Lưu file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.getFileName().toString();

        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file: " + e.getMessage());
        }
    }


    private void deletePhysicalFile(String path) {
        try {
            if (path != null) {
                Files.deleteIfExists(Paths.get(path));
            }
        } catch (IOException e) {
            System.err.println("⚠️ Không thể xoá file: " + e.getMessage());
        }
    }

    // SINH META
    private String generateMeta(String title) {
        if (title == null) return "tai-lieu";

        return Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]+", "")
                .replaceAll("[^a-zA-Z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .toLowerCase();
    }

    private String makeUniqueMeta(String baseMeta) {
        String meta = baseMeta;
        int counter = 1;
        while (documentRepository.existsByMeta(meta)) {
            meta = baseMeta + "-" + counter++;
        }
        return meta;
    }


    // LƯU DOCUMENT MỚI
    public void saveDocument(
            String title,
            String description,
            Long fileTypeId,
            Long categoryId,
            String meta,
            boolean isVisible,
            MultipartFile file,
            User creator) {

        try {
            if (meta == null || meta.trim().isEmpty()) {
                meta = generateMeta(title);
            }

            meta = makeUniqueMeta(meta);

            FileType fileType = fileTypeRepository.findById(fileTypeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại file"));

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File không hợp lệ");
            }

            // Lưu file gốc
            String fileName = storeFile(file);
            Path storedPath = Paths.get(UPLOAD_DIR + fileName);

            // Tạo thư mục PDF nếu chưa có
            File pdfFolder = new File(PDF_DIR);
            if (!pdfFolder.exists()) pdfFolder.mkdirs();

            // Convert sang PDF
            String inputPath = storedPath.toAbsolutePath().toString();
            String outputFolder = pdfFolder.getAbsolutePath();

            fileConvertService.convertToPdf(inputPath, outputFolder);

            // Tạo tên PDF đúng chuẩn
            String pdfName = fileName.replaceAll("\\.[^.]+$", "") + ".pdf";
            String pdfPath = Paths.get(PDF_DIR + pdfName).toAbsolutePath().toString();


            // LƯU DOCUMENT
            Document doc = new Document();
            doc.setTitle(title);
            doc.setDescription(description);
            doc.setFileType(fileType);
            doc.setCategory(category);
            doc.setMeta(meta);
            doc.setVisible(isVisible);
            doc.setFilename(fileName);
            doc.setMimeType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setFilePath(storedPath.toString());

            if (creator == null || creator.getId() == null) {
                throw new RuntimeException("User tạo tài liệu không hợp lệ");
            }


            User managedUser = userRepository.findById(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user tạo tài liệu"));

            doc.setCreatedBy(managedUser); // ⭐⭐⭐ BẮT BUỘC

            // PDF
            doc.setPdfFilename(pdfName);
            doc.setPdfPath(pdfPath);

            // Quyền xem
            doc.getVisibleToRoles().clear();

            if (isVisible) {
                // Công khai → EMPLOYEE + CEO
                doc.getVisibleToRoles().add(User.Role.EMPLOYEE);
                doc.getVisibleToRoles().add(User.Role.CEO);
            } else {
                // Riêng tư → chỉ người tạo
                doc.getVisibleToRoles().add(managedUser.getRole());
            }

            // Lưu DB
            documentRepository.save(doc);

            if (managedUser.isAdmin()) {
                notificationService.notifyDocumentUploaded(doc);
            }

            File pdfFile = new File(pdfPath);
            pdfPreviewService.generateFirstPagePreview(pdfFile, doc.getId());

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu tài liệu: " + e.getMessage(), e);
        }
    }


    // CẬP NHẬT DOCUMENT
    @Transactional
    public void updateDocument(
            Long id,
            String title,
            String description,
            Long fileTypeId,
            Long categoryId,
            String meta,
            boolean isVisible,
            MultipartFile file,
            User editor
    ) {

        //       VALIDATE EDITOR
        if (editor == null || editor.getId() == null) {
            throw new RuntimeException("Người chỉnh sửa không hợp lệ");
        }

        User managedEditor = userRepository.findById(editor.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người chỉnh sửa"));

        // LOAD DOCUMENT
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        // QUYỀN SỬA
        // Employee chỉ được sửa tài liệu của mình
        if (managedEditor.getRole() == User.Role.EMPLOYEE &&
                !doc.getCreatedBy().getId().equals(managedEditor.getId())) {
            throw new RuntimeException("Bạn không có quyền sửa tài liệu này");
        }

        // Không cho sửa nếu đã trình ký hoặc đã duyệt
        if (doc.getStatus() != DocumentStatus.DRAFT) {
            throw new RuntimeException("Không thể chỉnh sửa tài liệu sau khi đã trình ký");
        }

        //       UPDATE BASIC INFO
        doc.setTitle(title);
        doc.setDescription(description);
        doc.setMeta(meta);
        doc.setVisible(isVisible);

        doc.getVisibleToRoles().clear();

        if (isVisible) {
            doc.getVisibleToRoles().add(User.Role.EMPLOYEE);
            doc.getVisibleToRoles().add(User.Role.CEO);
        } else {
            doc.getVisibleToRoles().add(managedEditor.getRole());
        }

        FileType fileType = fileTypeRepository.findById(fileTypeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại file"));
        doc.setFileType(fileType);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
        doc.setCategory(category);

        //       FILE MỚI
        if (file != null && !file.isEmpty()) {

            // ❌ Xóa file cũ
            deletePhysicalFile(doc.getFilePath());
            deletePhysicalFile(doc.getPdfPath());

            // Xóa preview cũ
            pdfPreviewService.deleteAllPreviewsByDocumentId(doc.getId());

            // Lưu file mới
            String newFileName = storeFile(file);
            Path storedPath = Paths.get(UPLOAD_DIR + newFileName);

            // Convert PDF
            File pdfFolder = new File(PDF_DIR);
            if (!pdfFolder.exists()) pdfFolder.mkdirs();

            try {
                fileConvertService.convertToPdf(
                        storedPath.toAbsolutePath().toString(),
                        pdfFolder.getAbsolutePath()
                );
            } catch (Exception e) {
                throw new RuntimeException("Lỗi convert sang PDF", e);
            }

            String pdfName = newFileName.replaceAll("\\.[^.]+$", "") + ".pdf";
            String pdfPath = Paths.get(PDF_DIR + pdfName)
                    .toAbsolutePath().toString();

            doc.setFilename(newFileName);
            doc.setMimeType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setFilePath(storedPath.toString());
            doc.setPdfFilename(pdfName);
            doc.setPdfPath(pdfPath);

            // Generate preview mới
            pdfPreviewService.generateFirstPagePreview(
                    new File(pdfPath),
                    doc.getId()
            );
        }

        documentRepository.save(doc);
    }

    // xóa tài liệu
    @Transactional
    public void deleteById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        doc.setDeleted(true);
        doc.setDeletedAt(java.time.LocalDateTime.now());

        documentRepository.save(doc);
    }


    // tài liệu đã lưu
    public List<DocumentSearchDTO> getSavedDocuments(Long userId) {
        return getDocumentsByAction(
                userId,
                UserDocumentAction.ActionType.SAVED
        );
    }

    //tài liệu đã xem
    public List<DocumentSearchDTO> getRecentViewed(Long userId) {
        return getDocumentsByAction(
                userId,
                UserDocumentAction.ActionType.VIEWED
        );
    }

    private DocumentSearchDTO toDTO(Document d, Long userId) {
        return new DocumentSearchDTO(
                d,
                actionRepo.existsByUserIdAndDocumentIdAndActionType(userId, d.getId(), UserDocumentAction.ActionType.FAVORITE),
                actionRepo.existsByUserIdAndDocumentIdAndActionType(userId, d.getId(), UserDocumentAction.ActionType.SAVED),
                actionRepo.existsByUserIdAndDocumentIdAndActionType(userId, d.getId(), UserDocumentAction.ActionType.PINNED)
        );
    }

    public List<DocumentSearchDTO> getDocumentsByAction(Long userId, UserDocumentAction.ActionType type) {
        return actionRepo.findByUserIdAndActionType(userId, type)
                .stream()
                .map(a -> toDTO(a.getDocument(), userId))
                .toList();
    }

    // Tài liệu tải nhiều nhất
    public List<DocumentSearchDTO> getLatestDocumentsWithActions(int limit, Long userId) {
        return documentRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(d -> toDTO(d, userId))
                .toList();
    }

    // Tài liệu tải nhiều nhất
    public List<DocumentSearchDTO> getPopularDocumentsWithActions(int limit, Long userId) {

        List<DocumentSearchDTO> popular = downloadService.getTop5Downloaded();

        if (popular.isEmpty()) {
            return documentRepository.findTop20ByOrderByCreatedAtDesc()
                    .stream()
                    .limit(limit)
                    .map(d -> toDTO(d, userId))
                    .toList();
        }

        return popular.stream()
                .map(dto -> {
                    Document d = documentRepository.findById(dto.getId()).orElse(null);
                    if (d == null) return null;
                    DocumentSearchDTO nd = toDTO(d, userId);
                    nd.setDownloadCount(dto.getDownloadCount());
                    return nd;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // Tải tài liệu
    public ResponseEntity<Resource> downloadFile(Long docId) {

        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        Path filePath = Paths.get("uploads/original").resolve(doc.getFilename());

        try {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + doc.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Không đọc được file");
        }
    }


    // Lọc tài liệu
    public Page<Document> getDocumentsForAdmin(
            User admin,
            int page,
            int size,
            Long categoryId,
            Long fileTypeId,
            DocumentStatus status
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return documentRepository.findAdminUploadedDocuments(
                admin,
                categoryId,
                fileTypeId,
                status,
                pageable
        );
    }


    //Upload file tuwf user
    @Transactional
    public void uploadForUser(MultipartFile file, String title, User user) {

        if (user == null || user.getId() == null) {
            throw new RuntimeException("User không hợp lệ");
        }

        // mặc định cho USER
        Long defaultFileTypeId = fileTypeRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Chưa có loại file"))
                .getId();

        Long defaultCategoryId = categoryRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Chưa có danh mục"))
                .getId();

        saveDocument(
                title,
                null,
                defaultFileTypeId,
                defaultCategoryId,
                null,
                false,
                file,
                user
        );
    }


}
