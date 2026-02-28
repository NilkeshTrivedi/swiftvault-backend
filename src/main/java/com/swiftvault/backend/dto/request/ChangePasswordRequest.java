package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 50, message = "Password must be 6-50 characters")
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public String getNewPassword()     { return newPassword; }
    public void setCurrentPassword(String v) { this.currentPassword = v; }
    public void setNewPassword(String v)     { this.newPassword = v; }
}