package edu.uni.smartdocs.security;

import edu.uni.smartdocs.models.Category;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttribute {

    private final CategoryRepository categoryRepository;

    @ModelAttribute("isCEO")
    public boolean isCEO(Authentication authentication) {
        if (authentication == null) return false;
        if (authentication instanceof AnonymousAuthenticationToken) return false;

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.isCEO();
        }

        return false;
    }


    @ModelAttribute("allCategories")
    public List<Category> addCategoriesToModel() {
        return categoryRepository.findAll();
    }
}
