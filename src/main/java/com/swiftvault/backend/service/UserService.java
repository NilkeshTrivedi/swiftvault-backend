package com.swiftvault.backend.service;

import com.swiftvault.backend.dto.request.*;
import com.swiftvault.backend.dto.response.AuthResponse;
import com.swiftvault.backend.dto.response.UserResponse;
import com.swiftvault.backend.entity.User;

import java.util.List;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getProfile(String userId);
    void changePassword(String userId, ChangePasswordRequest request);
    void setTransactionPin(String userId, SetPinRequest request);
    void verifyTransactionPin(String userId, String pin);

    // Admin
    List<UserResponse> getAllUsers();
    List<UserResponse> searchUsers(String query);
    void suspendUser(String userId);
    void activateUser(String userId);
    void adminResetPassword(String targetUserId, String newPassword);

    User findById(String userId);
}