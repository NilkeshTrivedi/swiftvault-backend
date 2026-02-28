package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.ChangePasswordRequest;
import com.swiftvault.backend.dto.request.SetPinRequest;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.UserResponse;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @AuthenticationPrincipal User user
 * → Spring injects the currently logged-in user automatically.
 * The JwtAuthFilter set this in SecurityContext earlier.
 * No need to parse tokens manually here.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success("Profile fetched", userService.getProfile(user.getUserId())));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/me/pin")
    public ResponseEntity<ApiResponse<Void>> setTransactionPin(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SetPinRequest request) {
        userService.setTransactionPin(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Transaction PIN set successfully"));
    }
}