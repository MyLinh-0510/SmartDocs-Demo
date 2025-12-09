package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.models.Contact;
import edu.uni.smartdocs.models.ContactMessage;
import edu.uni.smartdocs.models.MessageDTO;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.ContactMessageRepository;
import edu.uni.smartdocs.repository.ContactRepository;
import edu.uni.smartdocs.service.ContactService;
import edu.uni.smartdocs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/user/contactu")
public class ContactUController {

    @Autowired
    private ContactRepository contactRepo;

    @Autowired
    private ContactMessageRepository messageRepo;

    @Autowired
    private ContactService contactService;

    @Autowired
    private UserService userService;


    // CLICK MENU → chuyển thẳng vào form
    @GetMapping
    public String defaultContact() {
        return "redirect:/user/contactu/form";
    }

    // HIỂN THỊ FORM LIÊN HỆ
    @GetMapping("/form")
    public String showContactForm(Model model, Principal principal) {

        Contact contact = new Contact();

        if (principal != null) {
            // Lấy user theo email đăng nhập
            User user = userService.findByEmail(principal.getName())
                    .orElse(null);

            if (user != null) {
                contact.setUserName(user.getFullName());
                contact.setUserEmail(user.getEmail());
                contact.setPhone(user.getPhone());
            }
        }

        model.addAttribute("contact", contact);
        return "user/contactu/form";
    }


    // USER GỬI LIÊN HỆ (tạo Contact + Message đầu tiên)
    @PostMapping("/form")
    public String submitContact(@ModelAttribute("contact") Contact contact,
                                @RequestParam("message") String firstMessage,
                                RedirectAttributes redirect,
                                Principal principal) {

        if (principal != null) {
            User user = userService.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

            contact.setUser(user);                // Gán user_id
            contact.setUserEmail(user.getEmail());
            contact.setUserName(user.getFullName());
        }

        contact.setReplied(false);
        contact.setCreatedAt(LocalDateTime.now());

        // Lưu Contact
        contact = contactRepo.save(contact);

        // Lưu Message đầu tiên
        ContactMessage msg = new ContactMessage();
        msg.setContact(contact);
        msg.setContent(firstMessage);
        msg.setFromAdmin(false);
        msg.setCreatedAt(LocalDateTime.now());

        messageRepo.save(msg);

        redirect.addFlashAttribute("success",
                "Gửi liên hệ thành công! Chúng tôi sẽ phản hồi sớm nhất.");

        return "redirect:/user/contactu/form";
    }


    // LỊCH SỬ LIÊN HỆ
    @GetMapping("/history")
    public String viewHistory(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        String email = principal.getName();
        List<Contact> history = contactService.getContactsByEmail(email);

        model.addAttribute("history", history);
        return "user/contactu/history";
    }

    @GetMapping("/messages/{contactId}")
    @ResponseBody
    public List<ContactMessage> getMessages(@PathVariable Long contactId, Principal principal) {

        Contact contact = contactRepo.findById(contactId).orElseThrow();

        // Chặn người xem không phải chủ sở hữu
        if (!contact.getUserEmail().equals(principal.getName())) {
            throw new RuntimeException("Bạn không có quyền xem cuộc trò chuyện này!");
        }

        return messageRepo.findByContactOrderByCreatedAtAsc(contact);
    }


    // USER HOẶC ADMIN GỬI TIN NHẮN MỚI
    @PostMapping("/message/send")
    @ResponseBody
    public String sendMessage(@RequestBody MessageDTO dto, Principal principal) {

        Contact contact = contactRepo.findById(dto.getContactId())
                .orElseThrow();

        ContactMessage msg = new ContactMessage();
        msg.setContact(contact);
        msg.setContent(dto.getContent());
        msg.setFromAdmin(dto.isAdmin());
        msg.setCreatedAt(LocalDateTime.now());

        messageRepo.save(msg);

        // Cập nhật trạng thái đã trả lời
        if (dto.isAdmin()) {
            contact.setReplied(true);
            contact.setReplyDate(LocalDateTime.now());
        } else {
            contact.setReplied(false);
        }

        contactRepo.save(contact);

        return "ok";
    }

}
