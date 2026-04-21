package com.workspace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // LocalDateTime 등 자바 8 날짜 타입을 처리하기 위한 모듈 등록
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}