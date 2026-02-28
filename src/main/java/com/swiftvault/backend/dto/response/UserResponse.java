package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.User;
import java.time.LocalDateTime;

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

    public UserResponse() {}

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.userId            = user.getUserId();
        r.fullName          = user.getFullName();
        r.email             = user.getEmail();
        r.phone             = user.getPhone();
        r.role              = user.getRole();
        r.status            = user.getStatus();
        r.hasTransactionPin = user.hasTransactionPin();
        r.lastLogin         = user.getLastLogin();
        r.createdAt         = user.getCreatedAt();
        return r;
    }

    public String          getUserId()           { return userId; }
    public String          getFullName()         { return fullName; }
    public String          getEmail()            { return email; }
    public String          getPhone()            { return phone; }
    public User.Role       getRole()             { return role; }
    public User.UserStatus getStatus()           { return status; }
    public boolean         isHasTransactionPin() { return hasTransactionPin; }
    public LocalDateTime   getLastLogin()        { return lastLogin; }
    public LocalDateTime   getCreatedAt()        { return createdAt; }
}