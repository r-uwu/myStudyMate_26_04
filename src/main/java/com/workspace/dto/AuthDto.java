package com.workspace.dto;

import lombok.Data;

public class AuthDto {

    @Data
    public static class SignupRequest {
        private String email;
        private String password;
        private String name;
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class TokenResponse {
        private String token;

        public TokenResponse(String token) {
            this.token = token;
        }
    }
}