package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.OpenRdRequest;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.RdResponse;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.RecurringDepositService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rd")
public class RecurringDepositController {

    private final RecurringDepositService rdService;

    public RecurringDepositController(RecurringDepositService rdService) {
        this.rdService = rdService;
    }

    // FIX #5: Changed @AuthenticationPrincipal from UserDetails to User entity
    // Same root cause as CardController/FD/Loan Controllers

    @PostMapping("/open")
    public ResponseEntity<ApiResponse<RdResponse>> openRd(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OpenRdRequest request) {
        RdResponse response = rdService.openRd(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Recurring Deposit opened successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RdResponse>>> getMyRds(
            @AuthenticationPrincipal User user) {
        List<RdResponse> rds = rdService.getMyRds(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("RDs retrieved", rds));
    }

    @GetMapping("/{rdId}")
    public ResponseEntity<ApiResponse<RdResponse>> getRd(
            @AuthenticationPrincipal User user,
            @PathVariable String rdId) {
        RdResponse rd = rdService.getRd(user.getUserId(), rdId);
        return ResponseEntity.ok(ApiResponse.success("RD retrieved", rd));
    }

    @PostMapping("/{rdId}/pay")
    public ResponseEntity<ApiResponse<RdResponse>> payInstallment(
            @AuthenticationPrincipal User user,
            @PathVariable String rdId,
            @RequestBody Map<String, String> body) {
        String pin = body.get("transactionPin");
        RdResponse rd = rdService.payInstallment(user.getUserId(), rdId, pin);
        return ResponseEntity.ok(ApiResponse.success("Installment paid successfully", rd));
    }

    @PostMapping("/{rdId}/close")
    public ResponseEntity<ApiResponse<RdResponse>> closeRd(
            @AuthenticationPrincipal User user,
            @PathVariable String rdId,
            @RequestBody Map<String, String> body) {
        String pin = body.get("transactionPin");
        RdResponse rd = rdService.closeRd(user.getUserId(), rdId, pin);
        return ResponseEntity.ok(ApiResponse.success("RD closed successfully", rd));
    }

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<Object>> getRdRates() {
        var info = new java.util.LinkedHashMap<String, String>();
        info.put("Interest Rate",      "6.50% p.a.");
        info.put("Minimum Installment","₹500 per month");
        info.put("Tenure",             "6 months to 10 years");
        info.put("Auto-Debit",         "Monthly on due date");
        info.put("Missed EMI Penalty", "₹50 per missed installment");
        info.put("Premature Closure",  "Principal returned, no interest");
        return ResponseEntity.ok(ApiResponse.success("RD information", info));
    }
}