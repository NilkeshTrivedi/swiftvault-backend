package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.User;

public class AuthResponse {
    private String token;
    private String tokenType;
    private String userId;
    private String fullName;
    private String email;
    private User.Role role;
    private boolean hasTransactionPin;

    public AuthResponse() {}

    public String    getToken()             { return token; }
    public String    getTokenType()         { return tokenType; }
    public String    getUserId()            { return userId; }
    public String    getFullName()          { return fullName; }
    public String    getEmail()             { return email; }
    public User.Role getRole()              { return role; }
    public boolean   isHasTransactionPin()  { return hasTransactionPin; }

    public void setToken(String v)             { this.token = v; }
    public void setTokenType(String v)         { this.tokenType = v; }
    public void setUserId(String v)            { this.userId = v; }
    public void setFullName(String v)          { this.fullName = v; }
    public void setEmail(String v)             { this.email = v; }
    public void setRole(User.Role v)           { this.role = v; }
    public void setHasTransactionPin(boolean v){ this.hasTransactionPin = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final AuthResponse r = new AuthResponse();
        public Builder token(String v)             { r.token = v; return this; }
        public Builder tokenType(String v)         { r.tokenType = v; return this; }
        public Builder userId(String v)            { r.userId = v; return this; }
        public Builder fullName(String v)          { r.fullName = v; return this; }
        public Builder email(String v)             { r.email = v; return this; }
        public Builder role(User.Role v)           { r.role = v; return this; }
        public Builder hasTransactionPin(boolean v){ r.hasTransactionPin = v; return this; }
        public AuthResponse build()                { return r; }
    }
}