package edu.uni.smartdocs.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final String UPLOAD_ROOT = System.getProperty("user.dir") + "/uploads/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        File uploadDir = new File(UPLOAD_ROOT);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Tạo các thư mục con cho Chat
        createChatDirectories();

        // Phục vụ tất cả file trong /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir.getAbsolutePath() + "/");
    }

    private void createChatDirectories() {
        String[] dirs = {
                "avatars",
                "chat/images",
                "chat/files",
                "chat/pdf"          // ← Chỉ tạo chat/pdf, không tạo pdf ở root nữa
        };

        for (String dir : dirs) {
            Path path = Paths.get(UPLOAD_ROOT, dir);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (Exception e) {
                    System.err.println("Không tạo được thư mục: " + path);
                }
            }
        }
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(50));
        factory.setMaxRequestSize(DataSize.ofMegabytes(50));
        return factory.createMultipartConfig();
    }
}