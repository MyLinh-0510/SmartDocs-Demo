package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.config.WebConfig;
import edu.uni.smartdocs.dto.ChatMessageDTO;
import edu.uni.smartdocs.dto.RecentContactDTO;
import edu.uni.smartdocs.models.ContactMessage;
import edu.uni.smartdocs.models.MessageType;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.ContactMessageRepository;
import edu.uni.smartdocs.service.FileConvertService;
import edu.uni.smartdocs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class ContactMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ContactMessageRepository messageRepo;
    private final UserService userService;
    private final FileConvertService fileConvertService;

    // ==================== 1. SEARCH USER ====================
    @GetMapping("/chat/search-user")
    @ResponseBody
    public List<RecentContactDTO> searchUser(@RequestParam String keyword) {
        List<User> users = userService.searchByEmailOrName(keyword);
        return users.stream().map(u -> {
            RecentContactDTO dto = new RecentContactDTO();
            dto.setOtherEmail(u.getEmail());
            dto.setOtherName(u.getFullName() != null ? u.getFullName() : u.getName());
            dto.setAvatar(u.getAvatar() != null ? u.getAvatar() : "default.jpg");
            dto.setLastMessage("");
            dto.setLastTime(LocalDateTime.now());
            return dto;
        }).toList();
    }

    // ==================== 2. RECENT CONTACTS ====================
    @GetMapping("/chat/recent-contacts")
    @ResponseBody
    public List<RecentContactDTO> getRecentContacts(Principal principal) {
        String myEmail = principal.getName();
        List<ContactMessage> allMessages = messageRepo.findAll();

        Map<String, ContactMessage> latestMap = new HashMap<>();

        for (ContactMessage m : allMessages) {
            if (Boolean.TRUE.equals(m.getDeleted())) continue;
            String other = m.getSenderEmail().equals(myEmail) ? m.getReceiverEmail() : m.getSenderEmail();
            if (!latestMap.containsKey(other) || latestMap.get(other).getCreatedAt().isBefore(m.getCreatedAt())) {
                latestMap.put(other, m);
            }
        }

        List<RecentContactDTO> result = latestMap.entrySet().stream()
                .map(e -> {
                    String otherEmail = e.getKey();
                    ContactMessage msg = e.getValue();
                    User u = userService.findByEmail(otherEmail).orElse(null);

                    RecentContactDTO dto = new RecentContactDTO();
                    dto.setOtherEmail(otherEmail);
                    dto.setOtherName(u != null ? (u.getFullName() != null ? u.getFullName() : u.getName()) : otherEmail);
                    dto.setAvatar(u != null && u.getAvatar() != null ? u.getAvatar() : "default.jpg");
                    dto.setLastTime(msg.getCreatedAt());

                    if (msg.getType() == MessageType.IMAGE) dto.setLastMessage("📷 Ảnh");
                    else if (msg.getType() == MessageType.FILE) dto.setLastMessage("📎 File");
                    else dto.setLastMessage(msg.getContent() != null ? msg.getContent() : "");

                    return dto;
                })
                .sorted(Comparator.comparing(RecentContactDTO::getLastTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // Self chat
        User me = userService.findByEmail(myEmail).orElse(null);
        RecentContactDTO self = new RecentContactDTO();
        self.setOtherEmail(myEmail);
        self.setOtherName("Ghi chú của tôi");
        self.setAvatar(me != null && me.getAvatar() != null ? me.getAvatar() : "default.jpg");
        self.setLastMessage("Chat với chính mình");
        self.setLastTime(LocalDateTime.now());

        List<RecentContactDTO> finalList = new ArrayList<>(result);
        finalList.add(0, self);
        return finalList;
    }

    // ==================== 3. HISTORY ====================
    @GetMapping("/chat/history")
    @ResponseBody
    public List<ChatMessageDTO> getHistory(@RequestParam String email, Principal principal) {

        String myEmail = principal.getName();

        List<ContactMessage> messages = messageRepo.findChat(myEmail, email);

        return messages.stream().map(m -> {

            ChatMessageDTO dto = new ChatMessageDTO();

            dto.setId(m.getId());

            dto.setSenderEmail(m.getSenderEmail());

            // ✅ FIX QUAN TRỌNG
            dto.setReceiverEmail(m.getReceiverEmail()); // ⬅️ phải là receiverEmail

            dto.setContent(m.getContent());

            dto.setType(
                    m.getType() != null ? m.getType().name() : "TEXT"
            );

            // ===== FILE / IMAGE =====
            dto.setFileUrl(m.getFileUrl());
            dto.setFileName(m.getFileName());

            dto.setFileUrls(
                    m.getFileUrls() != null ? m.getFileUrls() : new ArrayList<>()
            );

            dto.setFileNames(
                    m.getFileNames() != null ? m.getFileNames() : new ArrayList<>()
            );

            dto.setDownloadUrls(
                    (m.getDownloadUrls() != null && !m.getDownloadUrls().isEmpty())
                            ? m.getDownloadUrls()
                            : m.getFileUrls()
            );

            dto.setTimestamp(m.getCreatedAt());

            dto.setEdited(Boolean.TRUE.equals(m.getEdited()));
            dto.setDeleted(Boolean.TRUE.equals(m.getDeleted()));

            return dto;

        }).toList();
    }

    // ==================== 4. SEND MESSAGE ====================
    @MessageMapping("/chat.send")
    public void send(@Payload ChatMessageDTO dto, Principal principal) {
        String sender = principal.getName();
        String receiver = dto.getRecipientEmail();
        if (receiver == null || receiver.trim().isEmpty()) return;

        ContactMessage msg = new ContactMessage();
        msg.setSenderEmail(sender);
        msg.setReceiverEmail(receiver);
        msg.setContent(dto.getContent() != null ? dto.getContent() : "");
        msg.setType(MessageType.valueOf(dto.getType() != null ? dto.getType() : "TEXT"));

        msg.setFileUrl(dto.getFileUrl());
        msg.setFileName(dto.getFileName());
        msg.setFileUrls(dto.getFileUrls() != null ? dto.getFileUrls() : new ArrayList<>());
        msg.setFileNames(dto.getFileNames() != null ? dto.getFileNames() : new ArrayList<>());
        msg.setDownloadUrls(dto.getDownloadUrls() != null ? dto.getDownloadUrls() : new ArrayList<>());

        msg.setCreatedAt(LocalDateTime.now());
        msg.setDeleted(false);
        msg.setEdited(false);

        messageRepo.save(msg);

        ChatMessageDTO response = new ChatMessageDTO();
        response.setId(msg.getId());
        response.setSenderEmail(sender);
        response.setRecipientEmail(receiver);
        response.setContent(msg.getContent());
        response.setType(msg.getType().name());
        response.setFileUrl(msg.getFileUrl());
        response.setFileName(msg.getFileName());
        response.setFileUrls(msg.getFileUrls());
        response.setFileNames(msg.getFileNames());
        response.setDownloadUrls(msg.getDownloadUrls());
        response.setTimestamp(msg.getCreatedAt());
        response.setEdited(false);
        response.setDeleted(false);
        response.setFromAdmin(true);

        messagingTemplate.convertAndSendToUser(receiver, "/queue/messages", response);
        messagingTemplate.convertAndSendToUser(sender, "/queue/messages", response);
    }

    // ==================== UPLOAD MULTIPLE FILES ====================
    @PostMapping("/chat/upload-multiple")
    @ResponseBody
    public Map<String, Object> uploadMultiple(@RequestParam("files") MultipartFile[] files) throws Exception {

        List<String> viewUrls = new ArrayList<>();
        List<String> downloadUrls = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        boolean hasImage = false;

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            String contentType = file.getContentType();
            boolean isImage = contentType != null && contentType.startsWith("image/");

            if (isImage) hasImage = true;

            // ==================== IMAGE ====================
            if (isImage) {

                String subFolder = "chat/images/";
                Path uploadPath = Paths.get(WebConfig.UPLOAD_ROOT + subFolder);
                Files.createDirectories(uploadPath);

                // ✅ tên file không trùng
                String finalName = getUniqueFileName(uploadPath, originalName);

                Path savedPath = uploadPath.resolve(finalName);
                Files.copy(file.getInputStream(), savedPath, StandardCopyOption.REPLACE_EXISTING);

                String url = "/uploads/" + subFolder + finalName;

                viewUrls.add(url);
                downloadUrls.add(url);
                fileNames.add(finalName); // 👉 dùng tên thật đã xử lý
            }

            // ==================== FILE ====================
            else {

                String subFolder = "chat/files/";
                Path uploadPath = Paths.get(WebConfig.UPLOAD_ROOT + subFolder);
                Files.createDirectories(uploadPath);

                // 👉 tên file gốc (giữ nguyên + unique)
                String finalName = getUniqueFileName(uploadPath, originalName);

                Path savedPath = uploadPath.resolve(finalName);
                Files.copy(file.getInputStream(), savedPath, StandardCopyOption.REPLACE_EXISTING);

                // ===== PDF =====
                String pdfFolder = "chat/pdf/";
                Path pdfPath = Paths.get(WebConfig.UPLOAD_ROOT + pdfFolder);
                Files.createDirectories(pdfPath);

                // 👉 tạo tên PDF tương ứng
                String baseName = finalName.replaceAll("\\.[^.]+$", "");
                String pdfName = getUniqueFileName(pdfPath, baseName + ".pdf");

                Path pdfFilePath = pdfPath.resolve(pdfName);

                // 👉 convert TRỰC TIẾP ra file này
                fileConvertService.convertToPdfExact(
                        savedPath.toAbsolutePath().toString(),
                        pdfFilePath.toAbsolutePath().toString()
                );

                String pdfUrl = "/uploads/" + pdfFolder + pdfName;
                String originalUrl = "/uploads/" + subFolder + finalName;

                viewUrls.add(pdfUrl);
                downloadUrls.add(originalUrl);
                fileNames.add(finalName);
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("viewUrls", viewUrls);
        res.put("downloadUrls", downloadUrls);
        res.put("fileNames", fileNames);
        res.put("type", hasImage ? "IMAGE" : "FILE");

        return res;
    }

    private String getUniqueFileName(Path dir, String originalName) throws Exception {

        // 👉 giữ nguyên tên, chỉ replace ký tự nguy hiểm
        String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");

        Path filePath = dir.resolve(safeName);
        if (!Files.exists(filePath)) return safeName;

        String name = safeName;
        String ext = "";

        int dot = safeName.lastIndexOf(".");
        if (dot != -1) {
            name = safeName.substring(0, dot);
            ext = safeName.substring(dot);
        }

        int count = 1;
        while (true) {
            String newName = name + "(" + count + ")" + ext;
            Path newPath = dir.resolve(newName);

            if (!Files.exists(newPath)) {
                return newName;
            }
            count++;
        }
    }

    @PostMapping("/chat/edit")
    public ResponseEntity<?> editMessage(@RequestBody ChatMessageDTO dto, Principal principal) {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/chat/delete")
    public ResponseEntity<?> deleteMessage(@RequestBody ChatMessageDTO dto, Principal principal) {
        return ResponseEntity.ok("OK");
    }
}