package com.example.demo.repository;
import com.example.demo.domain.Explanation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExplanationRepository extends JpaRepository<Explanation, Long> {
}