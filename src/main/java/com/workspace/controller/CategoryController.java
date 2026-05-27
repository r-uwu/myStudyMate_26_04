package com.workspace.controller;

import com.workspace.entity.Category;
import com.workspace.entity.Explanation;
import com.workspace.repository.CategoryRepository;
import com.workspace.repository.ExplanationRepository;
import com.workspace.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ExplanationRepository explanationRepository;

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getUsername(); // JWT 토큰에서 가로챈 인증 이메일 추출
        return ResponseEntity.ok(categoryRepository.findByUserEmail(email));
    }

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody String name) {

        String email = userDetails.getUsername();
        Category category = Category.builder()
                .userEmail(email)
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