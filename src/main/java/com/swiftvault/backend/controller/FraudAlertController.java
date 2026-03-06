package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.entity.SuspiciousActivity;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.FraudDetectionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud")
public class FraudAlertController {

    private final FraudDetectionService fraudDetectionService;

    public FraudAlertController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    // ── Customer: see their own alerts ────────────────────────────────────────

    @GetMapping("/my-alerts")
    public ResponseEntity<ApiResponse<List<SuspiciousActivity>>> getMyAlerts(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Your security alerts",
                fraudDetectionService.getMyAlerts(user)));
    }

    // ── Admin: full management ────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<SuspiciousActivity>>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("All fraud alerts",
                fraudDetectionService.getAllAlerts(page, size)));
    }

    @GetMapping("/admin/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<SuspiciousActivity>>> getOpenAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Open fraud alerts",
                fraudDetectionService.getOpenAlerts(page, size)));
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success("Alert summary",
                fraudDetectionService.getAlertSummary()));
    }

    @PutMapping("/admin/{alertId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SuspiciousActivity>> resolveAlert(
            @AuthenticationPrincipal User admin,
            @PathVariable String alertId,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "RESOLVED");
        String notes  = body.getOrDefault("notes", "");
        return ResponseEntity.ok(ApiResponse.success("Alert updated",
                fraudDetectionService.resolveAlert(alertId, admin.getUserId(), status, notes)));
    }
}