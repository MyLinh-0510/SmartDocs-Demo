package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.service.DocumentIntelligenceService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/document-intelligence")
@CrossOrigin(origins = "*")
public class DocumentIntelligenceController {

    private final DocumentIntelligenceService intelligenceService;

    public DocumentIntelligenceController(DocumentIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @PostMapping("/process-all")
    public Map<String, Object> processAll() {
        intelligenceService.processAllApprovedDocuments();
        return Map.of("status", "started", "message", "Đang xử lý tài liệu");
    }
}
