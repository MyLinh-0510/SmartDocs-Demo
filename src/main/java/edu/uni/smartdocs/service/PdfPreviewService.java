package edu.uni.smartdocs.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PdfPreviewService {

    private static final int DPI = 150;

    private static final String PREVIEW_ROOT = "uploads/previews/";

    // ❌ XÓA TOÀN BỘ PREVIEW CŨ
    public void deleteAllPreviewsByDocumentId(Long docId) {
        try {
            Path previewDir = Paths.get(PREVIEW_ROOT + docId);
            if (Files.exists(previewDir)) {
                Files.walk(previewDir)
                        .sorted((a, b) -> b.compareTo(a)) // xóa file trước, thư mục sau
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {}
                        });
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không xóa được preview cũ: " + e.getMessage());
        }
    }

    // LUÔN TẠO LẠI PREVIEW MỚI
    public void generateFirstPagePreview(File pdfFile, Long docId) {
        try {
            Path previewDir = Paths.get(PREVIEW_ROOT + docId);
            Files.createDirectories(previewDir);

            Path output = previewDir.resolve("page-1.png");

            try (PDDocument document = PDDocument.load(pdfFile)) {
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage image =
                        renderer.renderImageWithDPI(0, DPI, ImageType.RGB);

                ImageIO.write(image, "png", output.toFile());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không tạo được preview PDF: " + e.getMessage());
        }
    }
}


