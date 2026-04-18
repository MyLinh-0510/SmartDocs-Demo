package edu.uni.smartdocs;

import edu.uni.smartdocs.service.DocumentIntelligenceService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class SmartdocsApplication {

	private final DocumentIntelligenceService intelligenceService;

	public SmartdocsApplication(DocumentIntelligenceService intelligenceService) {
		this.intelligenceService = intelligenceService;
	}

	public static void main(String[] args) {
		SpringApplication.run(SmartdocsApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		System.out.println("ỨNG DỤNG ĐÃ SẴN SÀNG, BẮT ĐẦU XỬ LÝ TÀI LIỆU...");
		// Chạy trong thread riêng để không block
		new Thread(() -> {
			try {
				Thread.sleep(5000); // Đợi 5 giây cho hệ thống ổn định
				intelligenceService.processAllApprovedDocuments();
			} catch (Exception e) {
				System.err.println("Lỗi xử lý: " + e.getMessage());
			}
		}).start();
		System.out.println("ĐÃ KHỞI TẠO TIẾN TRÌNH XỬ LÝ!");
	}
}
