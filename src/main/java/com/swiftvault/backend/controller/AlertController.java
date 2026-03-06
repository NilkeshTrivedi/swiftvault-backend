package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.entity.AlertThreshold;
import com.swiftvault.backend.entity.LowBalanceAlert;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.LowBalanceAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final LowBalanceAlertService alertService;

    public AlertController(LowBalanceAlertService alertService) {
        this.alertService = alertService;
    }

    // ── Low Balance Alerts ────────────────────────────────────────────────────

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<List<LowBalanceAlert>>> getMyAlerts(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Low balance alerts",
                alertService.getMyAlerts(user.getUserId())));
    }

    @GetMapping("/balance/unread")
    public ResponseEntity<ApiResponse<List<LowBalanceAlert>>> getUnreadAlerts(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Unread low balance alerts",
                alertService.getUnreadAlerts(user.getUserId())));
    }

    @GetMapping("/balance/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Unread count",
                Map.of("unreadCount", alertService.getUnreadCount(user.getUserId()))));
    }

    @PutMapping("/balance/mark-read")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal User user) {
        alertService.markAllRead(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("All alerts marked as read"));
    }

    // ── Threshold Configuration ───────────────────────────────────────────────

    @PutMapping("/threshold/{accountNumber}")
    public ResponseEntity<ApiResponse<AlertThreshold>> setThreshold(
            @AuthenticationPrincipal User user,
            @PathVariable String accountNumber,
            @RequestBody Map<String, Object> body) {
        BigDecimal threshold = new BigDecimal(body.get("threshold").toString());
        return ResponseEntity.ok(ApiResponse.success("Alert threshold configured",
                alertService.setThreshold(user.getUserId(), accountNumber, threshold)));
    }

    @GetMapping("/thresholds")
    public ResponseEntity<ApiResponse<List<AlertThreshold>>> getMyThresholds(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Your alert thresholds",
                alertService.getMyThresholds(user.getUserId())));
    }
}