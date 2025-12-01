package edu.uni.smartdocs.controllers;

import edu.uni.smartdocs.models.ContactMessage;
import edu.uni.smartdocs.repository.ContactMessageRepository;
import edu.uni.smartdocs.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/site/contact")
public class UserContactController {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private ContactService contactService;

    // Khi click menu "Liên hệ" → tự chuyển đến form
    @GetMapping
    public String defaultContact() {
        return "redirect:/site/contact/form";
    }

    // HIỂN THỊ FORM LIÊN HỆ
    @GetMapping("/form")
    public String showContactForm(Model model) {

        if (!model.containsAttribute("contactMessage")) {
            model.addAttribute("contactMessage", new ContactMessage());
        }

        return "site/contact/form";
    }

    // XỬ LÝ SUBMIT
    @PostMapping("/form")
    public String submitContact(@ModelAttribute ContactMessage contactMessage,
                                RedirectAttributes redirect) {

        contactMessageRepository.save(contactMessage);

        redirect.addFlashAttribute("success",
                "Gửi liên hệ thành công! Chúng tôi sẽ phản hồi sớm nhất.");

        return "redirect:/site/contact/form";
    }

    @GetMapping("/history")
    public String viewHistory(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String email = principal.getName(); // Lấy email người dùng đang đăng nhập

        List<ContactMessage> history = contactService.getContactsByEmail(email);

        model.addAttribute("history", history);
        return "site/contact/history";
    }
}
