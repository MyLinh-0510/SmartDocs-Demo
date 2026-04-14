package edu.uni.smartdocs.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

@Service
public class PdfTextExtractorService {

    public String extractTextFromPdf(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Xóa khoảng trắng thừa
            text = text.replaceAll("\\s+", " ").trim();

            return text;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String extractTextFromPdfByPath(String filePath) {
        try (PDDocument document = PDDocument.load(new java.io.File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}