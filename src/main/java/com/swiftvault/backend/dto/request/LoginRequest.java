package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public void setEmail(String v)    { this.email = v; }
    public void setPassword(String v) { this.password = v; }
}