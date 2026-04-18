package edu.uni.smartdocs.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class DocumentSummaryService {

    private final RestTemplate restTemplate;

    public DocumentSummaryService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(300000);

        this.restTemplate = new RestTemplate(factory);
        this.restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    // ===== PUBLIC =====

    public String summarize(File file) {
        try {
            if (file == null || !file.exists()) {
                return "File không tồn tại";
            }

            try (InputStream stream = new FileInputStream(file)) {
                return processSummarization(stream);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        }
    }

    // ===== CORE =====

    private String processSummarization(InputStream inputStream) throws Exception {

        String rawText = extractText(inputStream);

        // 🔥 LOG DEBUG QUAN TRỌNG
        System.out.println("==== RAW TEXT LENGTH: " + rawText.length());

        if (rawText == null || rawText.trim().isEmpty()) {
            return "Không đọc được nội dung PDF (file scan hoặc lỗi font)";
        }

        String cleaned = cleanText(rawText);

        int wordCount = cleaned.trim().split("\\s+").length;

        System.out.println("==== WORD COUNT: " + wordCount);

        if (wordCount < 20) {
            return "Nội dung quá ít để tóm tắt";
        }

        if (wordCount > 5000) {
            return "Văn bản quá dài (" + wordCount + " từ)";
        }

        String truncatedText = truncate(cleaned);

        return callPythonAI(truncatedText);
    }

    // ===== CALL AI =====

    private String callPythonAI(String text) {
        try {
            String url = "http://localhost:8000/summary";

            Map<String, String> request = Map.of("text", text);

            Map response = restTemplate.postForObject(url, request, Map.class);

            System.out.println("==== AI RESPONSE: " + response);

            if (response != null && response.get("summary") != null) {
                return response.get("summary").toString();
            }

            return "AI không trả về kết quả";

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi gọi AI: " + e.getMessage();
        }
    }

    // ===== CLEAN TEXT =====

    private String cleanText(String text) {
        if (text == null) return "";

        return text
                .replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncate(String text) {
        if (text == null) return "";

        String[] words = text.split("\\s+");
        int maxWords = 1200;

        if (words.length <= maxWords) return text;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            result.append(words[i]).append(" ");
        }
        return result.toString().trim();
    }

    // ===== EXTRACT TEXT =====

    private String extractText(InputStream stream) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            parser.parse(stream, handler, metadata, context);

            return handler.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}