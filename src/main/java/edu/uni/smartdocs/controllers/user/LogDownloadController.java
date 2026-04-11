package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.service.DocumentService;
import edu.uni.smartdocs.service.LogDownloadService;
import edu.uni.smartdocs.service.LogDownloadService;
import edu.uni.smartdocs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/downloads")
public class LogDownloadController {

    private final LogDownloadService logdownloadService;
    private final DocumentService documentService;
    private final UserService userService;

    @GetMapping("/{docId}")
    public ResponseEntity<?> download(
            @PathVariable Long docId,
            Principal principal
    ) {
        User user = userService.getCurrentUser(principal);

        // LƯU LOG DOWNLOAD
        logdownloadService.logDownload(user, docId);

        // TRẢ FILE
        return documentService.downloadFile(docId);
    }

    @GetMapping("/total")
    public long getTotalDownloads() {
        return logdownloadService.getTotalDownloads();
    }
}
