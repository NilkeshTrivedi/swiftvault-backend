package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "Password must be 6-50 characters")
    private String password;

    public String getFullName() { return fullName; }
    public String getEmail()    { return email; }
    public String getPhone()    { return phone; }
    public String getPassword() { return password; }
    public void setFullName(String v) { this.fullName = v; }
    public void setEmail(String v)    { this.email = v; }
    public void setPhone(String v)    { this.phone = v; }
    public void setPassword(String v) { this.password = v; }
}