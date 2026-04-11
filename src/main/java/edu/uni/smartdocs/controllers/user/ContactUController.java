package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.models.Contact;
import edu.uni.smartdocs.models.User;
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
    private ContactService contactService;

    @Autowired
    private UserService userService;

    // ================= MENU =================
    @GetMapping
    public String defaultContact() {
        return "redirect:/user/contactu/contact-page";
    }

    // ================= FORM =================
    @GetMapping("/contact-page")
    public String showContactForm(Model model, Principal principal) {

        Contact contact = new Contact();

        if (principal != null) {
            User user = userService.findByEmail(principal.getName()).orElse(null);

            if (user != null) {
                contact.setName(user.getFullName());
                contact.setEmail(user.getEmail());
                contact.setPhone(user.getPhone());
            }
        }

        model.addAttribute("contact", contact);
        return "/user/contactu/contact-page";
    }

    // ================= SUBMIT =================
    @PostMapping("/contact-page")
    public String submitContact(@ModelAttribute("contact") Contact contact,
                                RedirectAttributes redirect,
                                Principal principal) {

        if (contact.getMessage().length() > 1000) {
            redirect.addFlashAttribute("error", "Nội dung tối đa 1000 ký tự!");
            return "redirect:/user/contactu/contact-page";
        }

        if (principal != null) {
            User user = userService.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

            contact.setName(user.getFullName());
            contact.setEmail(user.getEmail());
            contact.setPhone(user.getPhone());
        }

        contact.setCreatedAt(LocalDateTime.now());
        contact.setReplied(false);

        contactRepo.save(contact);

        redirect.addFlashAttribute("success", "Gửi liên hệ thành công!");
        return "redirect:/user/contactu/contact-page";
    }

    // ================= HISTORY =================
    @GetMapping("/history")
    public String viewHistory(Model model, Principal principal) {

        if (principal == null) return "redirect:/login";

        String email = principal.getName();
        List<Contact> history = contactService.getContactsByEmail(email);

        model.addAttribute("history", history);
        return "user/contactu/history";
    }
}