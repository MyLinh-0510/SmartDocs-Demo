package edu.uni.smartdocs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uni.smartdocs.models.*;
import edu.uni.smartdocs.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DocumentIntelligenceService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentSummaryRepository summaryRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url:http://localhost:5001}")
    private String aiServiceUrl;

    public DocumentIntelligenceService(DocumentRepository documentRepository,
                                       DocumentChunkRepository chunkRepository,
                                       DocumentSummaryRepository summaryRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.summaryRepository = summaryRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void processAllApprovedDocuments() {
        List<Document> approvedDocs = documentRepository.findAll().stream()
                .filter(doc -> doc.getStatus() == DocumentStatus.APPROVED)
                .filter(doc -> chunkRepository.findByDocumentId(doc.getId()).isEmpty())
                .toList();

        System.out.println("📚 Tìm thấy " + approvedDocs.size() + " tài liệu cần xử lý");

        for (Document doc : approvedDocs) {
            try {
                processDocument(doc);
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("❌ Lỗi: " + e.getMessage());
            }
        }
    }

    private void processDocument(Document doc) {
        String filePath = findDocumentFile(doc);
        if (filePath == null) return;

        try {
            Path path = Paths.get(filePath);
            byte[] fileBytes = Files.readAllBytes(path);

            Map<String, Object> aiResponse = callAIProcess(fileBytes, path.getFileName().toString(), doc);

            if (aiResponse.containsKey("error")) return;

            chunkRepository.deleteByDocumentId(doc.getId());

            List<Map<String, Object>> chunks = (List<Map<String, Object>>) aiResponse.get("chunks");
            for (Map<String, Object> chunkData : chunks) {
                DocumentChunk chunk = DocumentChunk.builder()
                        .document(doc)
                        .chunkIndex((Integer) chunkData.get("index"))
                        .content((String) chunkData.get("content"))
                        .embedding(objectMapper.writeValueAsString(chunkData.get("embedding")))
                        .createdAt(LocalDateTime.now())
                        .build();
                chunkRepository.save(chunk);
            }

            System.out.println("✅ Đã xử lý: " + doc.getTitle() + " (" + chunks.size() + " chunks)");

        } catch (Exception e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }

    private String findDocumentFile(Document doc) {
        String uploadDir = "./uploads/pdf/";
        String[] paths = {
                uploadDir + doc.getPdfFilename(),
                uploadDir + doc.getFilename()
        };
        for (String path : paths) {
            if (path != null && new File(path).exists()) return path;
        }
        return null;
    }

    private Map<String, Object> callAIProcess(byte[] fileBytes, String filename, Document doc) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            org.springframework.util.MultiValueMap<String, Object> body =
                    new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", fileBytes);
            body.add("filename", filename);
            body.add("document_id", doc.getId().toString());
            body.add("title", doc.getTitle());

            HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceUrl + "/process-document",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }
}
