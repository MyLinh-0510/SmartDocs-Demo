package edu.uni.smartdocs.controllers.admin;

import edu.uni.smartdocs.models.Contact;
import edu.uni.smartdocs.models.ContactMessage;
import edu.uni.smartdocs.models.MessageDTO;
import edu.uni.smartdocs.repository.ContactMessageRepository;
import edu.uni.smartdocs.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/contact")
public class ContactAController {

    @Autowired
    private ContactRepository contactRepo;

    @Autowired
    private ContactMessageRepository messageRepo;

    // DANH SÁCH LIÊN HỆ
    @GetMapping("/index")
    public String listContacts(Model model) {
        model.addAttribute("contacts", contactRepo.findAll());
        return "admin/contact/index";
    }

    // CHI TIẾT LIÊN HỆ
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Contact contact = contactRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên hệ!"));

        model.addAttribute("contact", contact);
        return "admin/contact/detail";
    }

    // TRẢ LỜI LIÊN HỆ CŨ (GỬI MỘT LẦN)
    @GetMapping("/reply/{id}")
    public String replyForm(@PathVariable Long id, Model model) {
        Contact contact = contactRepo.findById(id)
                .orElseThrow();
        model.addAttribute("contact", contact);
        return "admin/contact/reply";
    }

    @PostMapping("/reply/{id}")
    public String sendReply(@PathVariable Long id,
                            @RequestParam String adminReply) {

        Contact contact = contactRepo.findById(id).orElseThrow();

        // Lưu lịch sử tin nhắn
        ContactMessage msg = new ContactMessage();
        msg.setContact(contact);
        msg.setFromAdmin(true);
        msg.setContent(adminReply);
        msg.setCreatedAt(LocalDateTime.now());
        messageRepo.save(msg);

        // Cập nhật contact
        contact.setAdminReply(adminReply);
        contact.setReplied(true);
        contact.setReplyDate(LocalDateTime.now());
        contactRepo.save(contact);

        return "redirect:/admin/contact/detail/" + id;
    }

    // API: LẤY LỊCH SỬ TIN NHẮN (JSON)
    @GetMapping("/messages/{contactId}")
    @ResponseBody
    public List<ContactMessage> getMessages(@PathVariable Long contactId) {

        Contact contact = contactRepo.findById(contactId).orElseThrow();
        return messageRepo.findByContactOrderByCreatedAtAsc(contact);
    }

    // API: LẤY THÔNG TIN LIÊN HỆ (ĐỂ LẤY TÊN USER)
    @GetMapping("/info/{contactId}")
    @ResponseBody
    public Contact getContactInfo(@PathVariable Long contactId) {
        return contactRepo.findById(contactId).orElseThrow();
    }


    // API: ADMIN GỬI TIN NHẮN (JSON)
    @PostMapping("/message/send")
    @ResponseBody
    public String sendAdminMessage(@RequestBody MessageDTO dto) {

        Contact contact = contactRepo.findById(dto.getContactId())
                .orElseThrow();

        ContactMessage msg = new ContactMessage();
        msg.setContact(contact);
        msg.setContent(dto.getContent());
        msg.setFromAdmin(true);
        msg.setCreatedAt(LocalDateTime.now());
        messageRepo.save(msg);

        // cập nhật trạng thái
        contact.setReplied(true);
        contact.setReplyDate(LocalDateTime.now());
        contactRepo.save(contact);

        return "ok";
    }
}
