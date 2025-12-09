package edu.uni.smartdocs.controllers.admin;

import edu.uni.smartdocs.models.Category;
import edu.uni.smartdocs.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories/index")
    public String listCategory(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories/index";
    }

    @GetMapping("/categories/create")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/categories/create";
    }

    @PostMapping("/categories/save")
    public String save(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("success", "Tạo danh mục thành công!");
        return "redirect:/admin/categories/index";
    }

    // Hiển thị form chỉnh sửa danh mục
    @GetMapping("/categories/edit/{id}")
    public String showEditCategoryForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Category> category = categoryService.findById(id);
        if (category.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Danh mục không tồn tại.");
            return "redirect:/admin/categories/index";
        }

        model.addAttribute("category", category.get());
        return "admin/categories/edit";
    }

    // Cập nhật thông tin danh mục
    @PostMapping("/categories/update/{id}")
    public String updateCategory(@PathVariable Long id, @ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        Optional<Category> existingCategory = categoryService.findById(id);

        if (existingCategory.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Danh mục không tồn tại.");
            return "redirect:/admin/categories/index";
        }

        Category updatedCategory = existingCategory.get();
        updatedCategory.setName(category.getName());
        updatedCategory.setDescription(category.getDescription());

        categoryService.save(updatedCategory);

        redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục thành công!");

        return "redirect:/admin/categories/index";
    }

    // Xóa danh mục
    @PostMapping("/categories/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Danh mục đã được xóa.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa danh mục.");
        }
        return "redirect:/admin/categories/index";
    }

}
