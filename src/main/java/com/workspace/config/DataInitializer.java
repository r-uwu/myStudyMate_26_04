package com.workspace.config;

import com.workspace.entity.Category;
import com.workspace.entity.Explanation;
import com.workspace.entity.User;
import com.workspace.repository.CategoryRepository;
import com.workspace.repository.ExplanationRepository;
import com.workspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExplanationRepository explanationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("test@email.com").isPresent()) {
            return; // 이미 데이터가 준비되어 있다면 중복 주입 방지
        }


        /*
        // 1. 테스트 유저 생성
        User testUser = User.builder()
                .email("test@email.com")
                .password(passwordEncoder.encode("password"))
                .name("서희")
                .role("ROLE_USER")
                .level(1)
                .exp(0)
                .build();
        userRepository.save(testUser);

         */

/*
        // 2. 테스트 카테고리 생성
        Category cat1 = Category.builder().sessionId("test@email.com").name("영어 시험범위").build();
        Category cat2 = Category.builder().sessionId("test@email.com").name("스프링부트 복습").build();
        categoryRepository.save(cat1);
        categoryRepository.save(cat2);

        // 3. 테스트 학습 요약 기록 데이터 적재
        Explanation exp1 = Explanation.builder()
                .topic("영어 관계대명사")
                .summary("관계대명사란 두 문장을 하나로 연결하는 접속사이자 대명사 역할을 한다.\n- 주격, 소유격, 목적격으로 분류됨.")
                .categoryId(cat1.getId())
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Explanation exp2 = Explanation.builder()
                .topic("스프링 시큐리티 필터 구조")
                .summary("OncePerRequestFilter는 요청당 단 한 번만 실행되는 필터 계층이다.\n- JWT 토큰 검증에 매우 적합함.")
                .categoryId(cat2.getId())
                .createdAt(LocalDateTime.now())
                .build();

        explanationRepository.save(exp1);
        explanationRepository.save(exp2);

 */
    }
}