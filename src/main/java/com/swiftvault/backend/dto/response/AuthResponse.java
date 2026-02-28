package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private String userId;
    private String fullName;
    private String email;
    private User.Role role;
    private boolean hasTransactionPin;
}