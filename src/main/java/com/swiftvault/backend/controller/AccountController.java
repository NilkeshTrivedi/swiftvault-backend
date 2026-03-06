package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.DepositRequest;
import com.swiftvault.backend.dto.request.OpenAccountRequest;
import com.swiftvault.backend.dto.request.TransferRequest;
import com.swiftvault.backend.dto.request.WithdrawRequest;
import com.swiftvault.backend.dto.response.AccountResponse;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.TransactionResponse;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.AccountService;
import com.swiftvault.backend.service.AccountSelfFreezeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService          accountService;
    private final AccountSelfFreezeService selfFreezeService;

    public AccountController(AccountService accountService,
                             AccountSelfFreezeService selfFreezeService) {
        this.accountService   = accountService;
        this.selfFreezeService = selfFreezeService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Open Account
    // POST /api/accounts
    // Body: { "accountType": "SAVINGS" }
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> openAccount(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OpenAccountRequest request) {
        AccountResponse account = accountService.openAccount(user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account opened successfully", account));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get My Accounts
    // GET /api/accounts
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Your accounts",
                accountService.getMyAccounts(user.getUserId())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Account Details
    // GET /api/accounts/{accountNumber}
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Account details",
                accountService.getAccount(user.getUserId(), accountNumber)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Balance
    // GET /api/accounts/{accountNumber}/balance
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber) {
        AccountResponse account = accountService.getAccount(user.getUserId(), accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Balance",
                Map.of(
                        "accountNumber", account.getAccountNumber(),
                        "balance",       account.getBalance(),
                        "accountType",   account.getAccountType(),
                        "status",        account.getStatus()
                )));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deposit
    // POST /api/accounts/{accountNumber}/deposit
    // Body: { "amount": 5000, "description": "Salary" }
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Deposit successful",
                accountService.deposit(user.getUserId(), accountNumber, request)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Withdraw
    // POST /api/accounts/{accountNumber}/withdraw
    // Body: { "amount": 1000, "transactionPin": "1234", "description": "ATM" }
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful",
                accountService.withdraw(user.getUserId(), accountNumber, request)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transfer
    // POST /api/accounts/{accountNumber}/transfer
    // Body: { "fromAccount": "ACC-XXX", "toAccount": "ACC-YYY",
    //         "amount": 5000, "transactionPin": "1234", "description": "Rent" }
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{accountNumber}/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @Valid @RequestBody TransferRequest request) {
        // Ensure the path account matches the request fromAccount
        if (!accountNumber.equals(request.getFromAccount()))
            request.setFromAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Transfer successful",
                accountService.transfer(user.getUserId(), request)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transaction History
    // GET /api/accounts/{accountNumber}/transactions
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionHistory(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Transaction history",
                accountService.getTransactionHistory(user.getUserId(), accountNumber)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Self-Freeze (Phase 3B)
    // POST /api/accounts/{accountNumber}/self-freeze
    // Body: { "transactionPin": "1234" }
    //
    // User freezes their own account immediately.
    // Use case: suspected compromise, lost credentials.
    // Also creates a ACCOUNT_SELF_FROZEN fraud alert for admin visibility.
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{accountNumber}/self-freeze")
    public ResponseEntity<ApiResponse<Void>> selfFreeze(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @RequestBody Map<String, String> body) {
        selfFreezeService.selfFreeze(user.getUserId(), accountNumber,
                body.get("transactionPin"));
        return ResponseEntity.ok(ApiResponse.success(
                "Account frozen. No further transactions are possible. " +
                        "Use self-unfreeze with your PIN to restore access."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Self-Unfreeze (Phase 3B)
    // POST /api/accounts/{accountNumber}/self-unfreeze
    // Body: { "transactionPin": "1234" }
    //
    // User unfreezes their own account.
    // Only works for self-frozen accounts — admin-frozen accounts
    // require admin action via PUT /api/accounts/{accountNumber}/unfreeze.
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{accountNumber}/self-unfreeze")
    public ResponseEntity<ApiResponse<Void>> selfUnfreeze(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @RequestBody Map<String, String> body) {
        selfFreezeService.selfUnfreeze(user.getUserId(), accountNumber,
                body.get("transactionPin"));
        return ResponseEntity.ok(ApiResponse.success("Account unfrozen. Transactions are now enabled."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — Freeze Account
    // PUT /api/accounts/{accountNumber}/freeze
    // Body: { "reason": "Suspicious activity detected" }
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{accountNumber}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> adminFreeze(
            @PathVariable String accountNumber,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Admin action") : "Admin action";
        return ResponseEntity.ok(ApiResponse.success("Account frozen by admin",
                accountService.freezeAccount(accountNumber, reason)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — Unfreeze Account
    // PUT /api/accounts/{accountNumber}/unfreeze
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{accountNumber}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> adminUnfreeze(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Account unfrozen by admin",
                accountService.unfreezeAccount(accountNumber)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — Get All Accounts
    // GET /api/accounts/admin/all
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.success("All accounts",
                accountService.getAllAccounts()));
    }
}