package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.Category;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.FileType;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.CategoryRepository;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.FileTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileTypeRepository fileTypeRepository;
    private final CategoryRepository categoryRepository;
    private final FileConvertService fileConvertService;

    private static final String UPLOAD_DIR = "uploads/original/";

    private static final String PDF_DIR = "uploads/pdf/";

    // ================== LẤY DANH SÁCH ==================
    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    public long count() {
        return documentRepository.count();
    }

    public Optional<Document> findById(Long id) {
        return documentRepository.findById(id);
    }

    public List<Document> findVisibleDocuments() {
        return documentRepository.findByIsVisibleTrue();
    }

    public List<Document> findByFileType(Long fileTypeId) {
        return documentRepository.findByFileType_Id(fileTypeId);
    }

    public List<Document> findByKeyword(String keyword) {
        return documentRepository.findByTitleContainingIgnoreCaseAndIsVisibleTrue(keyword);
    }

    public List<Document> getLatestVisibleDocuments() {
        return documentRepository.findTop20ByIsVisibleTrueOrderByCreatedAtDesc();
    }

    // ================== LƯU FILE ==================
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

    // ================== SINH META ==================
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

    // ================== LƯU DOCUMENT MỚI ==================
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

            // ======================
            // Tạo tên PDF đúng chuẩn
            // ======================
            String pdfName = fileName.replaceAll("\\.[^.]+$", "") + ".pdf";
            String pdfPath = Paths.get(PDF_DIR + pdfName).toAbsolutePath().toString();


            // Lưu vào DB
            Document doc = new Document();
            doc.setTitle(title);
            doc.setDescription(description);
            doc.setFileType(fileType);
            doc.setCategory(category);
            doc.setMeta(meta);
            doc.setIsVisible(isVisible);
            doc.setFilename(fileName);
            doc.setMimeType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setFilePath(storedPath.toString());

            // GÁN PDF FILE, RẤT QUAN TRỌNG
            doc.setPdfFilename(pdfName);
            doc.setPdfPath(pdfPath);

            // Gán quyền xem
            if (creator != null && creator.getRole() != null) {
                doc.getVisibleToRoles().add(creator.getRole());
            } else {
                doc.getVisibleToRoles().add(User.Role.EMPLOYEE);
            }

            documentRepository.save(doc);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu tài liệu: " + e.getMessage(), e);
        }
    }


    // ================== CẬP NHẬT DOCUMENT ==================
    public void updateDocument(
            Long id,
            String title,
            String description,
            Long fileTypeId,
            Long categoryId,
            String meta,
            boolean isVisible,
            MultipartFile file,
            User editor) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        doc.setTitle(title);
        doc.setDescription(description);
        doc.setMeta(meta);
        doc.setIsVisible(isVisible);

        FileType fileType = fileTypeRepository.findById(fileTypeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại file"));
        doc.setFileType(fileType);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
        doc.setCategory(category);

        // Nếu có file mới => xóa file cũ + lưu file mới
        if (file != null && !file.isEmpty()) {
            deletePhysicalFile(doc.getFilePath());
            String newFileName = storeFile(file);
            Path storedPath = Paths.get(UPLOAD_DIR + newFileName);

            doc.setFilename(newFileName);
            doc.setMimeType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setFilePath(storedPath.toString());
        }

        documentRepository.save(doc);
    }

    // ================== XOÁ DOCUMENT ==================
    public void deleteById(Long id) {
        Optional<Document> opt = documentRepository.findById(id);
        if (opt.isPresent()) {
            Document doc = opt.get();

            deletePhysicalFile(doc.getFilePath());

            documentRepository.deleteById(id);
        }
    }

}
