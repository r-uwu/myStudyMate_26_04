package com.workspace.controller;

import com.workspace.entity.Category;
import com.workspace.entity.Explanation;
import com.workspace.repository.CategoryRepository;
import com.workspace.repository.ExplanationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ExplanationRepository explanationRepository;

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories(@RequestParam String sessionId) {
        return ResponseEntity.ok(categoryRepository.findBySessionId(sessionId));
    }

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestParam String sessionId, @RequestBody String name) {
        // Remove quotes if the string is passed as a raw JSON string
        Category category = Category.builder()
                .sessionId(sessionId)
                .name(name.replace("\"", "").trim())
                .build();
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @PatchMapping("/history/{id}/category")
    public ResponseEntity<String> updateExplanationCategory(
            @PathVariable Long id,
            @RequestParam(required = false) Long categoryId) {

        Explanation explanation = explanationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Explanation not found"));

        explanation.updateCategory(categoryId);
        explanationRepository.save(explanation);

        return ResponseEntity.ok("Category updated successfully");
    }
}