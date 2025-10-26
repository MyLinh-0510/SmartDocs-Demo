package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.Category;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.FileType;
import edu.uni.smartdocs.repository.CategoryRepository;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.FileTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileTypeRepository fileTypeRepository;
    private final CategoryRepository categoryRepository;

    private static final String UPLOAD_DIR = "uploads/";

    // ===== Lấy toàn bộ tài liệu =====
    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    // ===== Tìm theo ID =====
    public Optional<Document> findById(Long id) {
        return documentRepository.findById(id);
    }

    // ===== Lưu hoặc cập nhật tài liệu =====
    public Document save(Document document) {
        return documentRepository.save(document);
    }

    // ===== Xóa tài liệu =====
    public void deleteById(Long id) {
        Optional<Document> opt = documentRepository.findById(id);
        if (opt.isPresent()) {
            Document doc = opt.get();
            if (doc.getFilePath() != null) {
                Path path = Paths.get(doc.getFilePath());
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    System.err.println("⚠️ Không thể xoá file: " + e.getMessage());
                }
            }
            documentRepository.deleteById(id);
        }
    }

    // ===== Tìm các tài liệu công khai =====
    public List<Document> findVisibleDocuments() {
        return documentRepository.findByIsVisibleTrue();
    }

    // ===== Tìm theo loại file =====
    public List<Document> findByFileType(Long fileTypeId) {
        return documentRepository.findByFileType_Id(fileTypeId);
    }

    // ===== Xử lý lưu tài liệu mới =====
    public void saveDocument(String title,
                             String description,
                             Long fileTypeId,
                             Long categoryId,
                             String meta,
                             boolean isVisible,
                             MultipartFile file) {

        try {
            // ✅ Tự sinh meta nếu rỗng
            if (meta == null || meta.trim().isEmpty()) {
                meta = generateMeta(title);
            }

            // ✅ Đảm bảo meta duy nhất
            meta = makeUniqueMeta(meta);

            // ✅ Tìm loại file và danh mục
            FileType fileType = fileTypeRepository.findById(fileTypeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại file"));
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

            // ✅ Lưu file vào thư mục uploads/
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                throw new RuntimeException("Tên file không hợp lệ");
            }

            // Tạo thư mục nếu chưa có
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Đặt tên file duy nhất
            String filename = System.currentTimeMillis() + "_" + originalName;
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // ✅ Tạo document mới
            Document doc = new Document();
            doc.setTitle(title);
            doc.setDescription(description);
            doc.setFileType(fileType);
            doc.setCategory(category);
            doc.setMeta(meta);
            doc.setIsVisible(isVisible);
            doc.setFilename(filename);
            doc.setOriginalName(originalName);
            doc.setMimeType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setFilePath(filePath.toString());

            documentRepository.save(doc);

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi lưu file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu tài liệu: " + e.getMessage(), e);
        }
    }

    // ✅ Sinh meta cơ bản từ tiêu đề
    private String generateMeta(String title) {
        if (title == null) return "tai-lieu";
        return Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]+", "")
                .replaceAll("[^a-zA-Z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .toLowerCase();
    }

    // ✅ Đảm bảo meta không trùng
    private String makeUniqueMeta(String baseMeta) {
        String meta = baseMeta;
        int counter = 1;
        while (documentRepository.existsByMeta(meta)) {
            meta = baseMeta + "-" + counter++;
        }
        return meta;
    }
}
