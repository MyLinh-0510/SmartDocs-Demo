package edu.uni.smartdocs.controllers.admin;

import edu.uni.smartdocs.models.Contact;
import edu.uni.smartdocs.repository.ContactMessageRepository;
import edu.uni.smartdocs.repository.ContactRepository;
import edu.uni.smartdocs.service.ContactService;
import edu.uni.smartdocs.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/contact")
public class ContactAController {

    @Autowired
    private ContactRepository contactRepo;

    @Autowired
    private ContactService contactService;

    @Autowired
    private EmailService emailService;

    // ================= DANH SÁCH =================
    @GetMapping("/index")
    public String listContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean replied,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Contact> contacts =
                contactService.filterContacts(replied, fromDate, toDate, pageable);

        model.addAttribute("contacts", contacts);
        model.addAttribute("replied", replied);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "admin/contact/index";
    }

    // ================= CHI TIẾT =================
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Contact contact = contactRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên hệ!"));

        model.addAttribute("contact", contact);
        return "admin/contact/detail";
    }

    // ================= FORM TRẢ LỜI =================
    @GetMapping("/reply/{id}")
    public String showReplyForm(@PathVariable Long id, Model model) {
        Contact contact = contactRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên hệ!"));

        model.addAttribute("contact", contact);
        return "admin/contact/reply";
    }

    // ================= LƯU TRẢ LỜI + GỬI EMAIL =================
    @PostMapping("/reply/{id}")
    public String saveReply(@PathVariable Long id,
                            @RequestParam("adminReply") String adminReply,
                            RedirectAttributes redirect) {

        Contact contact = contactRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên hệ!"));

        if (adminReply.length() > 1000) {
            redirect.addFlashAttribute("error", "Nội dung trả lời tối đa 1000 ký tự!");
            return "redirect:/admin/contact/reply/" + id;
        }

        contact.setAdminReply(adminReply);
        contact.setReplied(true);
        contact.setReplyDate(LocalDateTime.now());
        contactRepo.save(contact);

        // 👉 gửi email
        if (contact.getEmail() != null && !contact.getEmail().isBlank()) {

            String subject = "Phản hồi liên hệ từ SmartDocs";

            String content =
                    "Xin chào " + contact.getName() + ",\n\n"
                            + "Bạn đã gửi:\n"
                            + contact.getMessage() + "\n\n"
                            + "Phản hồi:\n"
                            + adminReply + "\n\n"
                            + "Trân trọng,\nSmartDocs";

            emailService.send(contact.getEmail(), subject, content);
        }

        redirect.addFlashAttribute("success", "Trả lời thành công!");
        return "redirect:/admin/contact/index";
    }

}