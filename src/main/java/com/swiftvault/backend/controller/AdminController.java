package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.response.AccountResponse;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.DashboardResponse;
import com.swiftvault.backend.dto.response.UserResponse;
import com.swiftvault.backend.service.AccountService;
import com.swiftvault.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService    userService;
    private final AccountService accountService;

    public AdminController(UserService userService, AccountService accountService) {
        this.userService    = userService;
        this.accountService = accountService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard data", accountService.getDashboard()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users fetched", userService.getAllUsers()));
    }

    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success("Search results", userService.searchUsers(q)));
    }

    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable String userId) {
        userService.suspendUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User suspended"));
    }

    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable String userId) {
        userService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User activated"));
    }

    @PutMapping("/users/{userId}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable String userId, @RequestBody Map<String, String> body) {
        userService.adminResetPassword(userId, body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.success("Accounts fetched", accountService.getAllAccounts()));
    }

    @PutMapping("/accounts/{accountNumber}/freeze")
    public ResponseEntity<ApiResponse<Void>> freezeAccount(@PathVariable String accountNumber) {
        // FIX: freezeAccount now returns AccountResponse — we discard it here, return void response
        accountService.freezeAccount(accountNumber, "Admin action");
        return ResponseEntity.ok(ApiResponse.success("Account frozen"));
    }

    @PutMapping("/accounts/{accountNumber}/unfreeze")
    public ResponseEntity<ApiResponse<Void>> unfreezeAccount(@PathVariable String accountNumber) {
        // FIX: unfreezeAccount now returns AccountResponse — discard it here
        accountService.unfreezeAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Account unfrozen"));
    }

    @PutMapping("/accounts/{accountNumber}/close")
    public ResponseEntity<ApiResponse<Void>> closeAccount(@PathVariable String accountNumber) {
        accountService.closeAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Account closed"));
    }

    @PostMapping("/operations/apply-interest")
    public ResponseEntity<ApiResponse<Void>> applyInterest() {
        accountService.applyMonthlyInterest();
        return ResponseEntity.ok(ApiResponse.success("Monthly interest applied"));
    }

    @PostMapping("/operations/apply-fees")
    public ResponseEntity<ApiResponse<Void>> applyFees() {
        accountService.applyLowBalanceFees();
        return ResponseEntity.ok(ApiResponse.success("Low balance fees applied"));
    }

    @PostMapping("/operations/export-csv")
    public ResponseEntity<ApiResponse<Void>> exportCsv() {
        String path = "exports/transactions_" + System.currentTimeMillis() + ".csv";
        accountService.exportTransactionsCsv(path);
        return ResponseEntity.ok(ApiResponse.success("Exported to: " + path));
    }
}