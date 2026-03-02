package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.FdWithdrawRequest;
import com.swiftvault.backend.dto.request.OpenFdRequest;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.FdResponse;
import com.swiftvault.backend.service.FixedDepositService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fd")
public class FixedDepositController {

    private final FixedDepositService fdService;

    public FixedDepositController(FixedDepositService fdService) {
        this.fdService = fdService;
    }

    @PostMapping("/open")
    public ResponseEntity<ApiResponse<FdResponse>> openFd(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OpenFdRequest request) {
        FdResponse response = fdService.openFd(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Fixed Deposit opened successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FdResponse>>> getMyFds(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<FdResponse> fds = fdService.getMyFds(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("FDs retrieved", fds));
    }

    @GetMapping("/{fdId}")
    public ResponseEntity<ApiResponse<FdResponse>> getFd(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String fdId) {
        FdResponse fd = fdService.getFd(userDetails.getUsername(), fdId);
        return ResponseEntity.ok(ApiResponse.success("FD retrieved", fd));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<FdResponse>> withdrawFd(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FdWithdrawRequest request) {
        FdResponse response = fdService.withdrawFd(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("FD withdrawal processed", response));
    }

    // Interest rate info - public endpoint
    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<Object>> getInterestRates() {
        var rates = new java.util.LinkedHashMap<String, String>();
        rates.put("7-29 days",    "4.00% p.a.");
        rates.put("30-90 days",   "5.50% p.a.");
        rates.put("91-180 days",  "6.00% p.a.");
        rates.put("181-365 days", "6.75% p.a.");
        rates.put("1-2 years",    "7.00% p.a.");
        rates.put("2-3 years",    "7.25% p.a.");
        rates.put("3+ years",     "7.50% p.a.");
        rates.put("Premature penalty", "1% on principal");
        rates.put("Compounding", "Quarterly");
        return ResponseEntity.ok(ApiResponse.success("FD interest rates", rates));
    }
}
