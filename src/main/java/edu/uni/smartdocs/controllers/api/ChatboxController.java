package edu.uni.smartdocs.controllers.api;

import edu.uni.smartdocs.dto.request.ChatRequestDTO;
import edu.uni.smartdocs.dto.response.ChatMessageDTO;
import edu.uni.smartdocs.service.SemanticSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatboxController {

    private final SemanticSearchService chatService;

    public ChatboxController(SemanticSearchService chatService) {
        this.chatService = chatService;
    }

    // ✅ Trả về trực tiếp ChatMessageDTO (không cần wrapper)
    @PostMapping("/send")
    public ChatMessageDTO sendMessage(@RequestBody ChatRequestDTO request) {
        System.out.println("📨 Nhận tin nhắn: " + request.getMessage());
        try {
            return chatService.processChatWithAI(request);
        } catch (Exception e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
            e.printStackTrace();
            // Trả về message lỗi
            ChatMessageDTO errorMsg = new ChatMessageDTO();
            errorMsg.setContent("❌ Lỗi: " + e.getMessage());
            errorMsg.setRole("BOT");
            errorMsg.setTimestamp(java.time.LocalDateTime.now());
            return errorMsg;
        }
    }

    // ✅ Trả về trực tiếp List<ChatMessageDTO>
    @GetMapping("/history")
    public List<ChatMessageDTO> getHistory(@RequestParam String sessionId) {
        try {
            return chatService.getChatHistory(sessionId);
        } catch (Exception e) {
            System.err.println("❌ Lỗi lấy lịch sử: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GetMapping("/sessions")
    public List<Map<String, Object>> getUserSessions(@RequestParam Long userId) {
        return chatService.getUserSessions(userId);
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId) {

        // Log để debug
        System.out.println("Đang xóa session: " + sessionId);

        boolean deleted = chatService.deleteChatSession(sessionId);

        if (deleted) {
            return ResponseEntity.ok().body(Map.of("message", "Session deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}