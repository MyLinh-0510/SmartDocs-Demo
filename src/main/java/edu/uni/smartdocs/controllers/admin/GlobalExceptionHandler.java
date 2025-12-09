package edu.uni.smartdocs.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException e,
                                      RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("error",
                "File quá lớn! Vui lòng chọn file nhỏ hơn giới hạn cho phép.");

        return "redirect:/admin/documents/create";
    }
}
