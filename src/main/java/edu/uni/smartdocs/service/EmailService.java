package edu.uni.smartdocs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetPasswordEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Đặt lại mật khẩu - Hệ thống quản trị");
        message.setText("Xin chào,\n\nVui lòng nhấp vào liên kết sau để đặt lại mật khẩu của bạn:\n"
                + resetLink + "\n\nLiên kết này có hiệu lực trong 24 giờ.\n\nTrân trọng,\nĐội ngũ hỗ trợ.");
        mailSender.send(message);
    }
}