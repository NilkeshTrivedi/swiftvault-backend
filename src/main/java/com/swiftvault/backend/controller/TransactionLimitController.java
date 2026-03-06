package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.entity.TransactionLimit;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.TransactionLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/limits")
public class TransactionLimitController {

    private final TransactionLimitService limitService;

    public TransactionLimitController(TransactionLimitService limitService) {
        this.limitService = limitService;
    }

    /**
     * GET /api/limits/{accountNumber}
     * Returns current limits for the account (or defaults if not configured).
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<TransactionLimit>> getLimits(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Transaction limits",
                limitService.getLimits(user.getUserId(), accountNumber)));
    }

    /**
     * GET /api/limits
     * Returns limits for all user's accounts.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionLimit>>> getAllMyLimits(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("All account limits",
                limitService.getAllMyLimits(user.getUserId())));
    }

    /**
     * PUT /api/limits/{accountNumber}
     * Update one or more limits. Send only the fields you want to change.
     *
     * Body example:
     * {
     *   "maxSingleTransaction": 25000,
     *   "dailyLimit": 50000,
     *   "monthlyLimit": 200000
     * }
     * Any field omitted keeps its existing value.
     */
    @PutMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<TransactionLimit>> updateLimits(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @RequestBody Map<String, Object> body) {

        BigDecimal maxSingle = body.containsKey("maxSingleTransaction")
                ? new BigDecimal(body.get("maxSingleTransaction").toString()) : null;
        BigDecimal daily     = body.containsKey("dailyLimit")
                ? new BigDecimal(body.get("dailyLimit").toString()) : null;
        BigDecimal monthly   = body.containsKey("monthlyLimit")
                ? new BigDecimal(body.get("monthlyLimit").toString()) : null;

        return ResponseEntity.ok(ApiResponse.success("Limits updated",
                limitService.updateLimits(user.getUserId(), accountNumber, maxSingle, daily, monthly)));
    }

    /**
     * GET /api/limits/{accountNumber}/summary
     * Returns today's and this month's spend vs limit — great for dashboard widget.
     *
     * Response includes:
     *   todaySpent, monthSpent, dailyRemaining, monthlyRemaining, dailyUsedPercent
     */
    @GetMapping("/{accountNumber}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSpendSummary(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Spend vs limit summary",
                limitService.getSpendSummary(user.getUserId(), accountNumber)));
    }
}