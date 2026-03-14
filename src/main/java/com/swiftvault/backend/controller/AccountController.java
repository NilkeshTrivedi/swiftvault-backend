package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.*;
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

    private final AccountService           accountService;
    private final AccountSelfFreezeService selfFreezeService;

    public AccountController(AccountService accountService,
                             AccountSelfFreezeService selfFreezeService) {
        this.accountService   = accountService;
        this.selfFreezeService = selfFreezeService;
    }

    // ── Open Account ──────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> openAccount(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account opened successfully",
                        accountService.openAccount(user.getUserId(), request)));
    }

    // ── Get My Accounts ───────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Your accounts",
                accountService.getMyAccounts(user.getUserId())));
    }

    // ── Get Account ───────────────────────────────────────────────────────────
    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Account details",
                accountService.getAccount(user.getUserId(), accountNumber)));
    }

    // ── Get Balance ───────────────────────────────────────────────────────────
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber) {
        AccountResponse account = accountService.getAccount(user.getUserId(), accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Balance",
                Map.of("accountNumber", account.getAccountNumber(),
                        "balance",       account.getBalance(),
                        "accountType",   account.getType(),
                        "status",        account.getStatus())));
    }

    // ── Set Nickname ──────────────────────────────────────────────────────────
    @PutMapping("/{accountNumber}/nickname")
    public ResponseEntity<ApiResponse<Void>> setNickname(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @RequestParam String nickname) {
        accountService.setNickname(user.getUserId(), accountNumber, nickname);
        return ResponseEntity.ok(ApiResponse.success("Nickname updated"));
    }

    // ── Deposit ───────────────────────────────────────────────────────────────
    // POST /api/accounts/{accountNumber}/deposit
    // Body: { amount, description? }
    // Note: accountNumber comes from URL — no need for it in body
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Deposit successful",
                accountService.deposit(user.getUserId(), accountNumber, request)));
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────
    // POST /api/accounts/{accountNumber}/withdraw
    // Body: { amount, transactionPin, description? }
    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful",
                accountService.withdraw(user.getUserId(), accountNumber, request)));
    }

    // ── Transfer ──────────────────────────────────────────────────────────────
    // POST /api/accounts/transfer
    // Body: { fromAccount, toAccount, amount, transactionPin, description? }
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Transfer successful",
                accountService.transfer(user.getUserId(), request)));
    }

    // ── Transaction History ───────────────────────────────────────────────────
    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getHistory(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @RequestParam(required = false) String type) {
        List<TransactionResponse> txns = (type != null)
                ? accountService.getFilteredHistory(user.getUserId(), accountNumber, type)
                : accountService.getTransactionHistory(user.getUserId(), accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Transactions", txns));
    }

    // ── Mini Statement ────────────────────────────────────────────────────────
    @GetMapping("/{accountNumber}/mini-statement")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getMiniStatement(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Mini statement",
                accountService.getMiniStatement(user.getUserId(), accountNumber)));
    }

    // ── Self-Freeze ───────────────────────────────────────────────────────────
    @PostMapping("/{accountNumber}/self-freeze")
    public ResponseEntity<ApiResponse<Void>> selfFreeze(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @RequestBody Map<String, String> body) {
        selfFreezeService.selfFreeze(user.getUserId(), accountNumber, body.get("transactionPin"));
        return ResponseEntity.ok(ApiResponse.success(
                "Account frozen. Use self-unfreeze with your PIN to restore access."));
    }

    // ── Self-Unfreeze ─────────────────────────────────────────────────────────
    @PostMapping("/{accountNumber}/self-unfreeze")
    public ResponseEntity<ApiResponse<Void>> selfUnfreeze(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @RequestBody Map<String, String> body) {
        selfFreezeService.selfUnfreeze(user.getUserId(), accountNumber, body.get("transactionPin"));
        return ResponseEntity.ok(ApiResponse.success("Account unfrozen successfully."));
    }

    // ── ADMIN — Freeze ────────────────────────────────────────────────────────
    // Interface: freezeAccount(String accountNumber, String reason) → AccountResponse
    @PutMapping("/{accountNumber}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> adminFreeze(
            @PathVariable String accountNumber,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null) ? body.getOrDefault("reason", "Admin action") : "Admin action";
        return ResponseEntity.ok(ApiResponse.success("Account frozen",
                accountService.freezeAccount(accountNumber, reason)));
    }

    // ── ADMIN — Unfreeze ──────────────────────────────────────────────────────
    // Interface: unfreezeAccount(String accountNumber) → AccountResponse
    @PutMapping("/{accountNumber}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> adminUnfreeze(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Account unfrozen",
                accountService.unfreezeAccount(accountNumber)));
    }

    // ── ADMIN — All Accounts ──────────────────────────────────────────────────
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.success("All accounts",
                accountService.getAllAccounts()));
    }
}