package com.workspace.controller;

import com.workspace.dto.AuthDto;
import com.workspace.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> register(@RequestBody AuthDto.SignupRequest request) {
        authService.register(request);
        return ResponseEntity.ok("Registration successful.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.TokenResponse> login(@RequestBody AuthDto.LoginRequest request) {
        AuthDto.TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}