package edu.uni.smartdocs.controllers.admin;

import edu.uni.smartdocs.config.WebConfig;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.DocumentRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentFileController {

    private static final Logger log = LoggerFactory.getLogger(DocumentFileController.class);

    @CrossOrigin(origins = "*")
    @GetMapping("/{filename:.+}")
    public void serveFile(@PathVariable String filename, HttpServletResponse response) throws IOException {
        Path filePath = Paths.get("uploads").resolve(filename).normalize();
        File file = filePath.toFile();

        if (!file.exists() || file.isDirectory()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("File không tồn tại trên server.");
            return;
        }

        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) {
            mimeType = detectMimeTypeByExtension(filename);
        }

        response.setContentType(mimeType);

        response.setHeader("Content-Disposition", "inline");

        response.setHeader("X-Frame-Options", "ALLOWALL");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Range");
        response.setHeader("Access-Control-Expose-Headers", "Content-Range, Accept-Ranges");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Ghi dữ liệu file ra output
        try (var inputStream = new FileSystemResource(file).getInputStream();
             var outputStream = response.getOutputStream()) {
            FileCopyUtils.copy(inputStream, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("Lỗi khi đọc file: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String detectMimeTypeByExtension(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (name.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (name.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (name.endsWith(".txt")) return "text/plain";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
