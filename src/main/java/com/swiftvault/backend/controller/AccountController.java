package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.*;
import com.swiftvault.backend.dto.response.AccountResponse;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.TransactionResponse;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Accounts fetched", accountService.getMyAccounts(user.getUserId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> openAccount(
            @AuthenticationPrincipal User user, @Valid @RequestBody OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account opened successfully", accountService.openAccount(user.getUserId(), request)));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @AuthenticationPrincipal User user, @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Account fetched", accountService.getAccount(user.getUserId(), accountNumber)));
    }

    @PutMapping("/{accountNumber}/nickname")
    public ResponseEntity<ApiResponse<Void>> setNickname(
            @AuthenticationPrincipal User user, @PathVariable String accountNumber, @RequestParam String nickname) {
        accountService.setNickname(user.getUserId(), accountNumber, nickname);
        return ResponseEntity.ok(ApiResponse.success("Nickname updated"));
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal User user, @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", accountService.deposit(user.getUserId(), request)));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @AuthenticationPrincipal User user, @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful", accountService.withdraw(user.getUserId(), request)));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal User user, @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Transfer successful", accountService.transfer(user.getUserId(), request)));
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getHistory(
            @AuthenticationPrincipal User user, @PathVariable String accountNumber,
            @RequestParam(required = false) String type) {
        List<TransactionResponse> txns = type != null
                ? accountService.getFilteredHistory(user.getUserId(), accountNumber, type)
                : accountService.getTransactionHistory(user.getUserId(), accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Transactions fetched", txns));
    }

    @GetMapping("/{accountNumber}/mini-statement")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getMiniStatement(
            @AuthenticationPrincipal User user, @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Mini statement fetched",
                accountService.getMiniStatement(user.getUserId(), accountNumber)));
    }
}