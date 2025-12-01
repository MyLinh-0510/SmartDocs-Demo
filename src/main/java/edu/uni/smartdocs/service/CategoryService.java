package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.Category;
import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    public List<Category> findAll() {
        return repo.findAll();
    }

    public Category save(Category category) {
        return repo.save(category);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public Optional<Category> findById(Long id) {
        return repo.findById(id);
    }
}
