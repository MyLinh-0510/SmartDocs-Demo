package edu.uni.smartdocs.service;

import edu.uni.smartdocs.dto.request.ChatRequestDTO;
import edu.uni.smartdocs.dto.response.ChatMessageDTO;
import edu.uni.smartdocs.dto.response.SemanticSearchResultDTO;
import edu.uni.smartdocs.models.ChatMessage;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.models.MessageType;
import edu.uni.smartdocs.models.DocumentStatus;
import edu.uni.smartdocs.models.Category;
import edu.uni.smartdocs.repository.CategoryRepository;
import edu.uni.smartdocs.repository.ChatMessageRepository;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SemanticSearchService {

    private final DocumentRepository documentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final CategoryRepository categoryRepository;

    // Cache cho danh sách category
    private List<String> cachedDocumentTypes = null;
    private List<Category> cachedCategories = null;
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION = 60000; // 1 phút

    private Integer extractMonth(String message) {
        for (int i = 1; i <= 12; i++) {
            if (message.contains("tháng " + i)) {
                return i;
            }
        }
        return null;
    }

    public SemanticSearchService(DocumentRepository documentRepository,
                                 ChatMessageRepository chatMessageRepository,
                                 UserRepository userRepository,
                                 CategoryRepository categoryRepository) {
        this.documentRepository = documentRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.restTemplate = new RestTemplate();
    }

    // ==================== LẤY DANH MỤC TỪ DATABASE ====================

    /**
     * Lấy danh sách tên category từ database (có cache)
     */
    private List<String> getSupportedDocumentTypes() {
        // Kiểm tra cache
        if (cachedDocumentTypes != null && (System.currentTimeMillis() - lastCacheTime) < CACHE_DURATION) {
            return cachedDocumentTypes;
        }

        List<Category> categories = categoryRepository.findAll();
        Set<String> typeNames = new HashSet<>();

        for (Category category : categories) {
            String name = category.getName().toLowerCase();
            // Thêm tên gốc
            typeNames.add(name);
            // Thêm tên không dấu
            String unsignName = removeDiacritics(name);
            if (!unsignName.equals(name)) {
                typeNames.add(unsignName);
            }
        }

        // Thêm từ khóa mặc định (fallback nếu database rỗng)
        if (typeNames.isEmpty()) {
            typeNames.addAll(getDefaultKeywords());
        }

        cachedDocumentTypes = new ArrayList<>(typeNames);
        lastCacheTime = System.currentTimeMillis();

        System.out.println("📚 Đã load " + cachedDocumentTypes.size() + " loại tài liệu từ database");
        return cachedDocumentTypes;
    }

    /**
     * Lấy danh sách category đầy đủ từ database (có cache)
     */
    private List<Category> getCategories() {
        if (cachedCategories != null && (System.currentTimeMillis() - lastCacheTime) < CACHE_DURATION) {
            return cachedCategories;
        }

        cachedCategories = categoryRepository.findAll();
        lastCacheTime = System.currentTimeMillis();
        return cachedCategories;
    }

    /**
     * Xóa dấu tiếng Việt
     */
    private String removeDiacritics(String text) {
        if (text == null) return "";

        String[] vietnameseChars = {
                "àáạảãâầấậẩẫăằắặẳẵ", "èéẹẻẽêềếệểễ", "ìíịỉĩ", "òóọỏõôồốộổỗơờớợởỡ",
                "ùúụủũưừứựửữ", "ỳýỵỷỹ", "đ", "ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ", "ÈÉẸẺẼÊỀẾỆỂỄ",
                "ÌÍỊỈĨ", "ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ", "ÙÚỤỦŨƯỪỨỰỬỮ", "ỲÝỴỶỸ", "Đ"
        };

        String[] englishChars = {
                "a", "e", "i", "o", "u", "y", "d",
                "A", "E", "I", "O", "U", "Y", "D"
        };

        String result = text;
        for (int i = 0; i < vietnameseChars.length; i++) {
            for (char c : vietnameseChars[i].toCharArray()) {
                result = result.replace(c, englishChars[i].charAt(0));
            }
        }
        return result;
    }

    /**
     * Từ khóa mặc định (fallback khi database rỗng)
     */
    private List<String> getDefaultKeywords() {
        return Arrays.asList(
                "tài liệu", "document", "văn bản", "van ban",
                "hợp đồng", "hop dong", "báo cáo", "bao cao",
                "hóa đơn", "hoa don", "công văn", "cong van",
                "quyết định", "quyet dinh", "thông báo", "thong bao"
        );
    }

    /**
     * Lấy icon cho category
     */
    private String getIconForCategory(String categoryName) {
        String name = categoryName.toLowerCase();
        if (name.contains("hợp đồng")) return "📄";
        if (name.contains("báo cáo")) return "📊";
        if (name.contains("hóa đơn")) return "🧾";
        if (name.contains("công văn")) return "📨";
        if (name.contains("quyết định")) return "⚖️";
        if (name.contains("thông báo")) return "🔔";
        if (name.contains("biên bản")) return "📝";
        if (name.contains("kế hoạch")) return "📅";
        if (name.contains("đào tạo")) return "📖";
        if (name.contains("kỹ thuật")) return "🔧";
        if (name.contains("nhân sự")) return "👥";
        if (name.contains("pháp lý")) return "⚖️";
        return "📁";
    }

    /**
     * Refresh cache (gọi khi category thay đổi)
     */
    public void refreshCategoryCache() {
        cachedDocumentTypes = null;
        cachedCategories = null;
        lastCacheTime = 0;
        System.out.println("🔄 Đã refresh cache danh mục tài liệu");
    }

    // ==================== TÌM KIẾM TÀI LIỆU ====================

    public List<SemanticSearchResultDTO> semanticSearch(String query, double threshold, int limit) {
        System.out.println("🔍 Semantic Search: " + query);

        User currentUser = getUser(1L);
        List<Document> documents = getDocumentsUserCanView(currentUser);
        List<SemanticSearchResultDTO> results = new ArrayList<>();

        if (documents.isEmpty()) {
            results.add(new SemanticSearchResultDTO(1L, "Hợp đồng lao động mẫu 2025",
                    "Hợp đồng lao động giữa công ty và nhân viên...", 0.95));
            results.add(new SemanticSearchResultDTO(2L, "Hướng dẫn sử dụng SmartDocs",
                    "Tài liệu hướng dẫn chi tiết cách sử dụng hệ thống SmartDocs", 0.88));
            results.add(new SemanticSearchResultDTO(3L, "Quy trình đăng ký tài liệu",
                    "Quy trình đăng ký tài liệu mới vào hệ thống", 0.82));
        } else {
            for (Document doc : documents) {
                double similarity = calculateSimilarity(query, doc.getTitle() + " " + doc.getDescription());
                if (similarity >= threshold) {
                    results.add(new SemanticSearchResultDTO(
                            doc.getId(),
                            doc.getTitle(),
                            doc.getDescription() != null ? doc.getDescription() : "Nội dung đang cập nhật",
                            similarity
                    ));
                }
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (results.size() > limit) {
            results = results.subList(0, limit);
        }

        return results;
    }

    // ==================== CHAT WITH AI ====================

    public ChatMessageDTO processChatWithAI(ChatRequestDTO request) {
        System.out.println("🧠 AI Chat - Nhận: " + request.getMessage());

        User user = getUser(request.getUserId());
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : "session_" + System.currentTimeMillis();

        List<ChatMessage> existingMsgs =
                chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        boolean isNewSession = existingMsgs.isEmpty();

        // ===== Lưu câu hỏi =====
        ChatMessage userMsg = ChatMessage.builder()
                .user(user)
                .userMessage(request.getMessage())
                .sessionId(sessionId)
                .sessionName(isNewSession ? generateSessionName(request.getMessage()) : null)
                .messageType(MessageType.USER)
                .timestamp(LocalDateTime.now())
                .build();

        ChatMessage savedUserMsg = chatMessageRepository.save(userMsg);

        // ===== Gọi AI =====
        Map<String, Object> aiResult = callAI(request.getMessage());
        String aiResponse = (String) aiResult.get("response");
        String intent = (String) aiResult.getOrDefault("intent", "unknown");

        System.out.println("🎯 Intent: " + intent);

        // ===== XỬ LÝ =====
        List<Document> documents = new ArrayList<>();
        String finalResponse = "";

        switch (intent) {

            case "search":

                // FIX 1: LẤY KEYWORD CHUẨN
                String keyword = extractKeywordFromMessage(request.getMessage());

                // FIX 2: LẤY THÁNG
                Integer month = extractMonth(request.getMessage());
                Integer year = null;

                System.out.println("🔎 Keyword: " + keyword);
                System.out.println("📅 Month: " + month);

                int offset = 0;
                int limit = 5;

                List<Document> allResults = searchDocumentsWithTime(user, keyword, month, year, 0, Integer.MAX_VALUE);

                documents = allResults.stream()
                        .limit(limit)
                        .toList();

                boolean hasMore = allResults.size() > limit;

                finalResponse = buildDocumentResponse(documents, keyword, aiResponse, user, hasMore);
                break;

            case "greeting":
            case "thank":
            case "goodbye":
            case "about_ai":
            case "help":
            case "permission":
                finalResponse = aiResponse;
                break;

            case "security_denied":
                finalResponse = aiResponse;
                break;

            case "unknown":
            default:

                String fallbackKeyword = extractKeywordFromMessage(request.getMessage());

                if (fallbackKeyword != null) {
                    System.out.println("⚠️ AI fail → fallback search");

                    Integer fallbackMonth = extractMonth(request.getMessage());

                    documents = searchDocumentsWithTime(user, fallbackKeyword, fallbackMonth, null, 0, 5);

                    boolean fallbackHasMore = documents.size() == 5;

                    finalResponse = buildDocumentResponse(
                            documents,
                            fallbackKeyword,
                            "🔍 Kết quả tìm kiếm:",
                            user,
                            fallbackHasMore
                    );
                }
        }

        // ===== Lưu response =====
        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        savedUserMsg.setBotResponse(finalResponse);
        savedUserMsg.setReferencedDocumentIdList(documentIds);
        savedUserMsg.setMessageType(MessageType.BOT);

        chatMessageRepository.save(savedUserMsg);

        // ===== Response =====
        ChatMessageDTO result = new ChatMessageDTO();
        result.setContent(finalResponse);
        result.setTimestamp(LocalDateTime.now());
        result.setRole("BOT");
        result.setSessionId(sessionId);
        result.setDocumentIds(documentIds);

        return result;
    }

    private String normalize(String text) {
        if (text == null) return "";
        return removeDiacritics(text.toLowerCase()).trim();
    }

    private boolean fuzzyMatch(String text, String keyword) {
        if (text.contains(keyword)) return true;

        String[] words = keyword.split("\\s+");

        for (String word : words) {
            if (word.length() < 2) continue;

            if (text.contains(word)) return true;

            for (String t : text.split("\\s+")) {
                if (levenshteinDistance(t, word) <= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else {
                    int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + cost
                    );
                }
            }
        }
        return dp[a.length()][b.length()];
    }
    private List<Document> searchDocumentsWithTime(
            User user,
            String keyword,
            Integer month,
            Integer year,
            int offset,
            int limit) {

        List<Document> allDocs = getDocumentsUserCanView(user);
        List<Document> results = new ArrayList<>();

        String keywordNorm = normalize(keyword);

        for (Document doc : allDocs) {

            System.out.println("👉 Checking: " + doc.getTitle());

            String titleNorm = normalize(doc.getTitle());
            String descNorm = normalize(doc.getDescription());
            String fileNorm = normalize(doc.getFilename());

            boolean matchKeyword;

            if (keyword == null || keyword.isBlank()) {
                matchKeyword = true;
            } else {
                matchKeyword =
                        fuzzyMatch(titleNorm, keywordNorm) ||
                                fuzzyMatch(descNorm, keywordNorm) ||
                                fuzzyMatch(fileNorm, keywordNorm);
            }

            boolean matchMonth = (month == null) ||
                    (doc.getCreatedAt() != null &&
                            doc.getCreatedAt().getMonthValue() == month);

            boolean matchYear = (year == null) ||
                    (doc.getCreatedAt() != null &&
                            doc.getCreatedAt().getYear() == year);

            System.out.println("   matchKeyword=" + matchKeyword +
                    ", matchMonth=" + matchMonth +
                    ", matchYear=" + matchYear);

            if (matchKeyword && matchMonth && matchYear) {
                results.add(doc);
            }
        }

        return results.stream()
                .sorted(Comparator.comparing(
                        Document::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String buildUnknownResponse(String aiResponse, String originalMessage) {
        StringBuilder response = new StringBuilder();

        response.append(aiResponse).append("\n\n");
        response.append("📋 **Dưới đây là các loại tài liệu có trong hệ thống:**\n\n");

        // Lấy danh sách category từ database
        List<Category> categories = getCategories();

        if (categories.isEmpty()) {
            // Fallback nếu chưa có dữ liệu
            response.append("   📄 Hợp đồng\n");
            response.append("   📊 Báo cáo\n");
            response.append("   🧾 Hóa đơn\n");
            response.append("   📨 Công văn\n");
            response.append("   ⚖️ Quyết định\n");
        } else {
            int index = 1;
            for (Category category : categories) {
                String icon = getIconForCategory(category.getName());
                response.append("   ").append(index++).append(". ")
                        .append(icon).append(" ").append(category.getName());

                if (category.getDescription() != null && !category.getDescription().isEmpty()) {
                    response.append(" - ").append(category.getDescription());
                }
                response.append("\n");
            }
        }

        response.append("\n💡 **Ví dụ câu hỏi:**\n");
        response.append("   • \"Tìm hợp đồng lao động\"\n");
        response.append("   • \"Cho tôi xem báo cáo tài chính\"\n");
        response.append("   • \"Tài liệu đào tạo nhân viên mới\"\n\n");

        response.append("🔐 **Lưu ý:** Tôi chỉ hiển thị tài liệu bạn có quyền xem (đã duyệt + công khai).\n");
        response.append("Hãy thử hỏi lại với nội dung cụ thể hơn nhé! 😊");

        return response.toString();
    }

    private List<Document> getDocumentsUserCanView(User user) {
        List<Document> allDocs = documentRepository.findAll();
        List<Document> accessibleDocs = new ArrayList<>();

        for (Document doc : allDocs) {
            if (canUserViewDocument(user, doc)) {
                accessibleDocs.add(doc);
            }
        }

        System.out.println("📊 User " + user.getEmail() + " có thể xem " + accessibleDocs.size() + " tài liệu");
        return accessibleDocs;
    }

    private boolean canUserViewDocument(User user, Document doc) {
        if (doc == null) return false;

        if (doc.isDeleted()) {
            System.out.println("   ❌ Doc " + doc.getId() + " - đã bị xóa");
            return false;
        }

        if (user.getRole() == User.Role.ADMIN) {
            if (doc.getStatus() == DocumentStatus.REJECTED) {
                System.out.println("   ❌ Doc " + doc.getId() + " - bị từ chối (REJECTED), không hiển thị");
                return false;
            }
            System.out.println("   ✅ Doc " + doc.getId() + " - ADMIN có quyền");
            return true;
        }

        if (doc.getStatus() == DocumentStatus.REJECTED) {
            System.out.println("   ❌ Doc " + doc.getId() + " - bị từ chối (REJECTED), không hiển thị");
            return false;
        }

        if (doc.getStatus() != DocumentStatus.APPROVED) {
            System.out.println("   ❌ Doc " + doc.getId() + " - chưa được duyệt (status=" + doc.getStatus() + ")");
            return false;
        }

        if (!doc.isVisible()) {
            System.out.println("   ❌ Doc " + doc.getId() + " - chưa được công khai (is_visible=false)");
            return false;
        }

        if (doc.getVisibleToRoles() != null && !doc.getVisibleToRoles().isEmpty()) {
            if (!doc.getVisibleToRoles().contains(user.getRole())) {
                System.out.println("   ❌ Doc " + doc.getId() + " - role " + user.getRole() + " không có quyền");
                return false;
            }
        }

        System.out.println("   ✅ Doc " + doc.getId() + " - " + doc.getTitle());
        return true;
    }

    private List<Document> searchDocumentsUserCanView(User user, String keyword, int offset, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Document> allAccessibleDocs = getDocumentsUserCanView(user);
        List<Document> results = new ArrayList<>();
        String keywordLower = keyword.toLowerCase();

        for (Document doc : allAccessibleDocs) {
            boolean matchInTitle = doc.getTitle() != null && doc.getTitle().toLowerCase().contains(keywordLower);
            boolean matchInDesc = doc.getDescription() != null && doc.getDescription().toLowerCase().contains(keywordLower);
            boolean matchInFilename = doc.getFilename() != null && doc.getFilename().toLowerCase().contains(keywordLower);
            boolean matchInPdfFilename = doc.getPdfFilename() != null && doc.getPdfFilename().toLowerCase().contains(keywordLower);

            if (matchInTitle || matchInDesc || matchInFilename || matchInPdfFilename) {
                results.add(doc);
            }
        }

        if (results.isEmpty()) {
            String[] keywords = keywordLower.split("\\s+");
            Set<Document> uniqueResults = new HashSet<>();
            for (String kw : keywords) {
                if (kw.length() > 2) {
                    for (Document doc : allAccessibleDocs) {
                        if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(kw)) {
                            uniqueResults.add(doc);
                        } else if (doc.getDescription() != null && doc.getDescription().toLowerCase().contains(kw)) {
                            uniqueResults.add(doc);
                        }
                    }
                }
            }
            results = new ArrayList<>(uniqueResults);
        }

        results.sort(Comparator.comparing(Document::getId).reversed());

        System.out.println("📄 Tìm thấy " + results.size() + " tài liệu cho từ khóa: " + keyword);

        int start = Math.min(offset, results.size());
        int end = Math.min(offset + limit, results.size());

        if (start >= end) {
            return new ArrayList<>();
        }

        return results.subList(start, end);
    }

    private String buildDocumentResponse(List<Document> documents, String keyword, String aiResponse, User user, boolean hasMore) {
        StringBuilder response = new StringBuilder();

        if (documents.isEmpty()) {
            response.append("😞 Không tìm thấy tài liệu nào liên quan đến \"").append(keyword).append("\".\n");
            response.append("💡 Gợi ý: Hãy thử tìm với từ khóa khác.\n");
            response.append("📌 Lưu ý: Chỉ hiển thị tài liệu đã được duyệt, công khai và bạn có quyền xem.");
        } else {
            // CHỈ HIỂN THỊ TỐI ĐA 5 TÀI LIỆU
            int displayLimit = 5;
            List<Document> displayDocs = documents.size() > displayLimit
                    ? documents.subList(0, displayLimit)
                    : documents;

            response.append("✨ Tìm thấy ").append(documents.size()).append(" tài liệu:\n\n");

            for (int i = 0; i < displayDocs.size(); i++) {
                Document doc = displayDocs.get(i);

                String fileName = doc.getPdfFilename();
                if (fileName == null || fileName.isEmpty()) {
                    fileName = doc.getFilename();
                }
                if (fileName == null || fileName.isEmpty()) {
                    fileName = doc.getTitle() + ".pdf";
                }

                String fileLink = null;
                if (doc.getPdfFilename() != null && !doc.getPdfFilename().isEmpty()) {
                    fileLink = "/uploads/pdf/" + doc.getPdfFilename();
                } else if (doc.getFilename() != null && !doc.getFilename().isEmpty()) {
                    fileLink = "/uploads/pdf/" + doc.getFilename();
                }

                response.append(i + 1).append(". ").append(doc.getTitle()).append("\n");
                response.append("   Mô tả: ").append(doc.getDescription() != null ?
                        (doc.getDescription().length() > 100 ? doc.getDescription().substring(0, 100) + "..." : doc.getDescription()) :
                        "Nội dung đang cập nhật").append("\n");

                if (fileLink != null) {
                    response.append("   Xem chi tiết: <a href=\"").append(fileLink).append("\" target=\"_blank\">").append(fileName).append("</a>\n\n");
                } else {
                    response.append("   Xem chi tiết: /documents/").append(doc.getId()).append("\n\n");
                }
            }

            response.append("💡 Click vào link để xem hoặc tải tài liệu!\n");

            // NẾU CÓ NHIỀU HƠN 5 TÀI LIỆU, THÊM LINK "XEM THÊM"
            if (documents.size() > displayLimit) {
                int remainingCount = documents.size() - displayLimit;
                response.append("\n👉 **Còn ").append(remainingCount)
                        .append(" tài liệu nữa.** <a href=\"/documents/search?keyword=")
                        .append(keyword)
                        .append("\" target=\"_blank\">Xem thêm...</a>");
            }
        }

        return response.toString();
    }

    private double calculateSimilarity(String query, String content) {
        if (query == null || content == null) return 0;
        String[] queryWords = query.toLowerCase().split("\\s+");
        String contentLower = content.toLowerCase();
        int matchCount = 0;
        for (String word : queryWords) {
            if (contentLower.contains(word)) {
                matchCount++;
            }
        }
        return (double) matchCount / queryWords.length;
    }

    private Map<String, Object> callAI(String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("message", message);
            body.put("session_id", "default");

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:5000/chat",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> result = response.getBody();
            System.out.println("📨 AI Response: " + result);

            if (!result.containsKey("intent")) {
                result.put("intent", "unknown");
            }
            if (!result.containsKey("should_search")) {
                result.put("should_search", false);
            }

            return result;

        } catch (Exception e) {
            System.err.println("❌ Lỗi gọi AI: " + e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("response", createFallbackResponseText(message));
            fallback.put("should_search", false);
            fallback.put("intent", "unknown");
            return fallback;
        }
    }

    private String createFallbackResponseText(String message) {
        String msgLower = message.toLowerCase();
        if (msgLower.contains("chào") || msgLower.contains("xin")) {
            return "👋 Xin chào! Tôi là trợ lý AI của SmartDocs. Tôi có thể giúp bạn tìm kiếm tài liệu trong hệ thống.";
        } else if (msgLower.contains("cảm ơn")) {
            return "🙏 Không có gì! Rất vui được giúp bạn!";
        } else if (msgLower.contains("tạm biệt")) {
            return "👋 Tạm biệt bạn! Hẹn gặp lại!";
        } else {
            return "🔍 Tôi đang tìm kiếm tài liệu cho bạn...";
        }
    }

    private String extractKeywordFromMessage(String message) {
        if (message == null) return null;

        String msgLower = message.toLowerCase();
        String msgUnsign = removeDiacritics(msgLower);

        List<String> supportedTypes = getSupportedDocumentTypes();

        for (String docType : supportedTypes) {
            String typeLower = docType.toLowerCase();
            String typeUnsign = removeDiacritics(typeLower);

            if (msgLower.contains(typeLower) || msgUnsign.contains(typeUnsign)) {
                System.out.println("Tìm thấy từ khóa: " + docType);
                return docType;
            }
        }

        return null;
    }

    public List<ChatMessageDTO> getChatHistory(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        List<ChatMessageDTO> history = new ArrayList<>();

        for (ChatMessage msg : messages) {
            ChatMessageDTO userDto = new ChatMessageDTO();
            userDto.setContent(msg.getUserMessage());
            if (msg.getTimestamp() != null) {
                userDto.setTimestamp(msg.getTimestamp());
            }
            userDto.setRole("USER");
            userDto.setSessionId(msg.getSessionId());
            history.add(userDto);

            if (msg.getBotResponse() != null && !msg.getBotResponse().isEmpty()) {
                ChatMessageDTO botDto = new ChatMessageDTO();
                botDto.setContent(msg.getBotResponse());
                if (msg.getTimestamp() != null) {
                    botDto.setTimestamp(msg.getTimestamp());
                }
                botDto.setRole("BOT");
                botDto.setSessionId(msg.getSessionId());
                botDto.setDocumentIds(msg.getReferencedDocumentIdList());
                history.add(botDto);
            }
        }
        return history;
    }

    public List<Map<String, Object>> getUserSessions(Long userId) {
        User user = getUser(userId);
        List<Object[]> sessions = chatMessageRepository.findDistinctSessionsByUser(user);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] session : sessions) {
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", session[0]);
            sessionInfo.put("sessionName", session[1] != null ? session[1] : "Cuộc trò chuyện mới");
            sessionInfo.put("createdAt", session[2]);
            result.add(sessionInfo);
        }
        return result;
    }

    private String generateSessionName(String firstMessage) {
        if (firstMessage == null || firstMessage.isEmpty()) {
            return "Cuộc trò chuyện mới";
        }
        String name = firstMessage.length() > 50 ? firstMessage.substring(0, 50) : firstMessage;
        return name;
    }

    private User getUser(Long userId) {
        if (userId != null) {
            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) return user.get();
        }
        User defaultUser = userRepository.findById(1L).orElse(null);
        if (defaultUser == null) {
            defaultUser = new User();
            defaultUser.setId(1L);
            defaultUser.setEmail("default@smartdocs.com");
            defaultUser.setName("Default User");
            defaultUser.setRole(User.Role.EMPLOYEE);
        }
        return defaultUser;
    }


    @Transactional
    public boolean deleteChatSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            System.out.println("❌ SessionId không hợp lệ");
            return false;
        }

        try {
            // Xóa tất cả tin nhắn thuộc session này
            int deletedCount = chatMessageRepository.deleteBySessionId(sessionId);

            System.out.println("🗑️ Đã xóa " + deletedCount + " tin nhắn của session: " + sessionId);

            return deletedCount > 0;

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi xóa session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


}