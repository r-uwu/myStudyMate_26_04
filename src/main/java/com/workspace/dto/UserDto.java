package com.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String email;
        private String name;
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    public static class ProfileUpdateRequest {
        private String name;
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    public static class PasswordUpdateRequest {
        private String currentPassword;
        private String newPassword;
    }
}