package com.workspace.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.workspace.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 및 뷰 파일 전체 개방
                        .requestMatchers("/", "/index.html", "/login.html", "/summary.html", "/dashboard.html","/settings.html", "/intro.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico","/api/v1/users/").permitAll()
                        // 내부 에러 라우팅 개방
                        .requestMatchers("/error").permitAll()
                        // 인증 API 개방
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 그 외 모든 학습 코어 API는 인증 필수
                        .requestMatchers("/api/v1/learning/**").authenticated()
                        .requestMatchers("/api/v1/learning/stats/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}