package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.CreateAutoSavingsRuleRequest;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.AutoSavingsRuleResponse;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.AutoSavingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auto-savings")
public class AutoSavingsController {

    private final AutoSavingsService autoSavingsService;

    public AutoSavingsController(AutoSavingsService autoSavingsService) {
        this.autoSavingsService = autoSavingsService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AutoSavingsRuleResponse>> createRule(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateAutoSavingsRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Auto-savings rule created",
                autoSavingsService.createRule(user.getUserId(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AutoSavingsRuleResponse>>> getMyRules(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Your auto-savings rules",
                autoSavingsService.getMyRules(user.getUserId())));
    }

    @PutMapping("/{ruleId}/toggle")
    public ResponseEntity<ApiResponse<AutoSavingsRuleResponse>> toggleRule(
            @AuthenticationPrincipal User user,
            @PathVariable String ruleId,
            @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(ApiResponse.success(
                active ? "Rule activated" : "Rule paused",
                autoSavingsService.toggleRule(user.getUserId(), ruleId, active)));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @AuthenticationPrincipal User user,
            @PathVariable String ruleId) {
        autoSavingsService.deleteRule(user.getUserId(), ruleId);
        return ResponseEntity.ok(ApiResponse.success("Rule deleted"));
    }
}