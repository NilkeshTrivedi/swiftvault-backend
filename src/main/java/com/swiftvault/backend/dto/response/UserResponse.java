package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * What we return when someone asks for user info.
 * Never exposes passwordHash or pinHash.
 */
@Data
@Builder
public class UserResponse {
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private User.Role role;
    private User.UserStatus status;
    private boolean hasTransactionPin;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .hasTransactionPin(user.hasTransactionPin())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}