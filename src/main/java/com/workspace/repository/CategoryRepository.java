package com.workspace.repository;

import com.workspace.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    //List<Category> findBySessionId(String sessionId);
    List<Category> findByUserEmail(String userEmail);
}