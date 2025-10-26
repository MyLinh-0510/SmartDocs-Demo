package edu.uni.smartdocs.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String showIndexPage() {
        return "index"; // ✅ file index.html trong templates/
    }
}