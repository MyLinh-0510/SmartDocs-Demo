package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.ShareLink;
import edu.uni.smartdocs.models.ShareType;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.ShareLinkRepository;
import edu.uni.smartdocs.repository.UserRepository;
import edu.uni.smartdocs.service.EmailService;
import edu.uni.smartdocs.service.ShareLinkService;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ShareController {

    private final ShareLinkService shareLinkService;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ShareLinkRepository shareLinkRepository;


    /* SHARE PRIVATE qua email */
    @PostMapping("/user/share/{docId}/email")
    @ResponseBody
    public ResponseEntity<?> shareToEmail(
            @PathVariable Long docId,
            @RequestParam String email,
            Principal principal) {

        Document doc = documentRepository.findById(docId).orElseThrow();
        String ownerEmail = principal.getName();

        String link = shareLinkService.createPrivateShare(doc, ownerEmail, email);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi đến email: " + email,
                "link", link
        ));
    }

    /* SHARE PUBLIC */
    @PostMapping("/user/share/{docId}")
    @ResponseBody
    public ResponseEntity<?> createPublicShare(
            @PathVariable Long docId,
            @RequestParam int minutes,
            Principal principal) {

        Document doc = documentRepository.findById(docId).orElseThrow();

        String link = shareLinkService.createPublicShare(
                doc,
                principal.getName(),
                minutes
        );

        return ResponseEntity.ok(Map.of("link", link));
    }


    /* Khi người dùng mở link */
    @GetMapping("/share/{token}")
    public String access(@PathVariable String token, Principal principal, Model model) {

        ShareLink link = shareLinkService.getByToken(token);

        if (link == null) {
            model.addAttribute("message", "Link không tồn tại");
            return "error-page";
        }

        if (link.getExpiryTime() != null && link.getExpiryTime().isBefore(LocalDateTime.now())) {
            model.addAttribute("message", "Liên kết đã hết hạn");
            return "error-page";
        }

        // ✅ PUBLIC
        if (link.getType() == ShareType.PUBLIC) {
            model.addAttribute("document", link.getDocument());
            model.addAttribute("token", token); // 🔥 BẮT BUỘC
            return "user/documentsu/share-view";
        }

        // PRIVATE nhưng đã có link → cho xem luôn
        if (link.getType() == ShareType.PRIVATE) {
            model.addAttribute("document", link.getDocument());
            model.addAttribute("token", token);
            return "user/documentsu/share-view";
        }

        // ❌ Sai quyền
        model.addAttribute("document", link.getDocument());
        model.addAttribute("ownerEmail", link.getOwnerEmail());
        model.addAttribute("token", token);

        return "user/documentsu/request-access";
    }

    @GetMapping("/view/pdf/{id}")
    public ResponseEntity<Resource> viewPdf(
            @PathVariable Long id,
            @RequestParam(required = false) String token) throws IOException {

        Document doc = documentRepository.findById(id).orElseThrow();

        // ✅ CHECK TOKEN
        if (token != null) {
            ShareLink link = shareLinkRepository.findByToken(token).orElse(null);

            if (link == null || !link.getDocument().getId().equals(id)) {
                return ResponseEntity.status(403).build();
            }

            if (link.getExpiryTime() != null &&
                    link.getExpiryTime().isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(403).build();
            }
        }

        Path path = Paths.get("uploads/pdf/" + doc.getPdfFilename());
        Resource resource = new UrlResource(path.toUri());

        // ✅ FIX LỖI TIẾNG VIỆT (CỰC QUAN TRỌNG)
        String encodedFileName = java.net.URLEncoder
                .encode(doc.getPdfFilename(), java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }


    /* Gửi request truy cập */
    @PostMapping("/share/request/{token}")
    public String requestAccess(
            @PathVariable String token,
            @RequestParam String email,
            Model model) {

        ShareLink link = shareLinkService.getByToken(token);

        model.addAttribute("message", "Yêu cầu đã được gửi");
        return "user/documentsu/request-sent";
    }
}