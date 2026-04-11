package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.*;
import edu.uni.smartdocs.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final EmailService emailService;

    /* Tạo link PRIVATE: chỉ đúng email xem được */
    public String createPrivateShare(Document doc, String ownerEmail, String allowedEmail) {
        String token = UUID.randomUUID().toString();

        ShareLink link = new ShareLink();
        link.setToken(token);
        link.setDocument(doc);
        link.setOwnerEmail(ownerEmail);
        link.setAllowedEmail(allowedEmail);
        link.setType(ShareType.PRIVATE);
        link.setExpiryTime(null);

        shareLinkRepository.save(link);

        String url = "http://localhost:8080/share/" + token;

        emailService.send(
                allowedEmail,
                "Bạn được chia sẻ tài liệu",
                "Bạn được cấp quyền xem tài liệu: " + doc.getTitle() +
                        "\n\nMở tài liệu: " + url
        );

        return url;
    }

    /* Tạo link PUBLIC: ai có link cũng xem */
    public String createPublicShare(Document doc, String owner, int minutes) {
        String token = UUID.randomUUID().toString();

        ShareLink link = new ShareLink();
        link.setToken(token);
        link.setDocument(doc);
        link.setOwnerEmail(owner);
        link.setAllowedEmail(null);
        link.setType(ShareType.PUBLIC);
        link.setExpiryTime(LocalDateTime.now().plusMinutes(minutes));

        shareLinkRepository.save(link);

        return "http://localhost:8080/share/" + token;
    }

    public ShareLink getByToken(String token) {
        return shareLinkRepository.findByToken(token).orElse(null);
    }
}