package edu.uni.smartdocs.controllers.api;

import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.service.DocumentSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class DocumentSummaryController {

    private final DocumentSummaryService summaryService;
    private final DocumentRepository documentRepository;

    // Chuyển sang GetMapping để tránh lỗi CSRF (403 Forbidden)
    @GetMapping("/summarize/{id}")
    public ResponseEntity<?> summarize(@PathVariable Long id) {
        try {
            Document doc = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

            // Xử lý đường dẫn file
            Path filePath = Paths.get("src/main/resources/static/uploads/pdf/", doc.getPdfFilename());
            File file = filePath.toFile();

            if (!file.exists()) {
                file = new File("uploads/pdf/" + doc.getPdfFilename());
            }

            if (!file.exists()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy file PDF: " + file.getAbsolutePath()));
            }

            String summaryText = summaryService.summarize(file);

            // Trả về JSON chuẩn
            return ResponseEntity.ok(Map.of("summary", summaryText));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Lỗi khi tóm tắt tài liệu",
                    "message", e.getMessage()
            ));
        }
    }
}

