package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email", name = "uk_users_email")
        }
)
public class User implements UserDetails {

    @Id
    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "pin_hash", length = 255)
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─── Enums ───────────────────────────────────────────────────────────────

    public enum Role { CUSTOMER, ADMIN }

    public enum UserStatus { ACTIVE, SUSPENDED, LOCKED }

    // ─── Constructors ─────────────────────────────────────────────────────────

    public User() {}

    public User(String userId, String fullName, String email, String phone,
                String passwordHash, String pinHash, Role role, UserStatus status,
                int failedAttempts, LocalDateTime lockedUntil,
                LocalDateTime lastLogin, LocalDateTime createdAt) {
        this.userId          = userId;
        this.fullName        = fullName;
        this.email           = email;
        this.phone           = phone;
        this.passwordHash    = passwordHash;
        this.pinHash         = pinHash;
        this.role            = role;
        this.status          = status;
        this.failedAttempts  = failedAttempts;
        this.lockedUntil     = lockedUntil;
        this.lastLogin       = lastLogin;
        this.createdAt       = createdAt;
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = UserStatus.ACTIVE;
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String getUserId()           { return userId; }
    public String getFullName()         { return fullName; }
    public String getEmail()            { return email; }
    public String getPhone()            { return phone; }
    public String getPasswordHash()     { return passwordHash; }
    public String getPinHash()          { return pinHash; }
    public Role getRole()               { return role; }
    public UserStatus getStatus()       { return status; }
    public int getFailedAttempts()      { return failedAttempts; }
    public LocalDateTime getLockedUntil()  { return lockedUntil; }
    public LocalDateTime getLastLogin()    { return lastLogin; }
    public LocalDateTime getCreatedAt()    { return createdAt; }

    // ─── Setters ─────────────────────────────────────────────────────────────

    public void setUserId(String userId)               { this.userId = userId; }
    public void setFullName(String fullName)           { this.fullName = fullName; }
    public void setEmail(String email)                 { this.email = email; }
    public void setPhone(String phone)                 { this.phone = phone; }
    public void setPasswordHash(String passwordHash)   { this.passwordHash = passwordHash; }
    public void setPinHash(String pinHash)             { this.pinHash = pinHash; }
    public void setRole(Role role)                     { this.role = role; }
    public void setStatus(UserStatus status)           { this.status = status; }
    public void setFailedAttempts(int failedAttempts)  { this.failedAttempts = failedAttempts; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public void setLastLogin(LocalDateTime lastLogin)     { this.lastLogin = lastLogin; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    // ─── Business Logic ───────────────────────────────────────────────────────

    public boolean isAccountLocked() {
        if (status != UserStatus.LOCKED) return false;
        if (lockedUntil != null && LocalDateTime.now().isAfter(lockedUntil)) {
            this.status         = UserStatus.ACTIVE;
            this.failedAttempts = 0;
            this.lockedUntil    = null;
            return false;
        }
        return true;
    }

    public boolean hasTransactionPin() {
        return pinHash != null && !pinHash.isBlank();
    }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String userId;
        private String fullName;
        private String email;
        private String phone;
        private String passwordHash;
        private String pinHash;
        private Role role;
        private UserStatus status = UserStatus.ACTIVE;
        private int failedAttempts = 0;
        private LocalDateTime lockedUntil;
        private LocalDateTime lastLogin;
        private LocalDateTime createdAt;

        public Builder userId(String userId)               { this.userId = userId; return this; }
        public Builder fullName(String fullName)           { this.fullName = fullName; return this; }
        public Builder email(String email)                 { this.email = email; return this; }
        public Builder phone(String phone)                 { this.phone = phone; return this; }
        public Builder passwordHash(String passwordHash)   { this.passwordHash = passwordHash; return this; }
        public Builder pinHash(String pinHash)             { this.pinHash = pinHash; return this; }
        public Builder role(Role role)                     { this.role = role; return this; }
        public Builder status(UserStatus status)           { this.status = status; return this; }
        public Builder failedAttempts(int failedAttempts)  { this.failedAttempts = failedAttempts; return this; }
        public Builder lockedUntil(LocalDateTime v)        { this.lockedUntil = v; return this; }
        public Builder lastLogin(LocalDateTime v)          { this.lastLogin = v; return this; }
        public Builder createdAt(LocalDateTime v)          { this.createdAt = v; return this; }

        public User build() {
            return new User(userId, fullName, email, phone, passwordHash, pinHash,
                    role, status, failedAttempts, lockedUntil, lastLogin, createdAt);
        }
    }

    // ─── UserDetails (Spring Security) ───────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String  getPassword()                 { return passwordHash; }
    @Override public String  getUsername()                 { return email; }
    @Override public boolean isAccountNonExpired()         { return true; }
    @Override public boolean isAccountNonLocked()          { return !isAccountLocked(); }
    @Override public boolean isCredentialsNonExpired()     { return true; }
    @Override public boolean isEnabled()                   { return status == UserStatus.ACTIVE; }
}