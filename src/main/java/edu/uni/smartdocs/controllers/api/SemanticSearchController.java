package edu.uni.smartdocs.controllers.api;

import edu.uni.smartdocs.dto.response.SemanticSearchResultDTO;
import edu.uni.smartdocs.dto.SearchRequest;
import edu.uni.smartdocs.service.SemanticSearchService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/semantic-search")
@CrossOrigin(origins = "*")
public class SemanticSearchController {

    private final SemanticSearchService searchService;

    public SemanticSearchController(SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public Map<String, String> test() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "API đang hoạt động!");
        result.put("message", "Dùng POST để tìm kiếm ngữ nghĩa");
        return result;
    }

    @PostMapping
    public List<SemanticSearchResultDTO> search(@RequestBody SearchRequest request) {
        try {
            System.out.println("📥 Nhận request: " + request.getQuery());
            return searchService.semanticSearch(
                    request.getQuery(),
                    request.getThreshold() > 0 ? request.getThreshold() : 0.7,
                    request.getLimit() > 0 ? request.getLimit() : 5
            );
        } catch (Exception e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}