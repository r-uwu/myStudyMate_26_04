package com.workspace.controller;

import com.workspace.dto.UserDto;
import com.workspace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto.Response> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        /* userDetails.getUsername() typically returns the email used during authentication */
        UserDto.Response response = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<Void> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserDto.ProfileUpdateRequest request) {
        userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserDto.PasswordUpdateRequest request) {
        userService.updatePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteUser(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}