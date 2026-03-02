package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.LoanApplicationRequest;
import com.swiftvault.backend.dto.request.PayEmiRequest;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.LoanResponse;
import com.swiftvault.backend.entity.Loan;
import com.swiftvault.backend.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LoanResponse>> applyForLoan(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LoanApplicationRequest request) {
        LoanResponse response = loanService.applyForLoan(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Loan application submitted successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getMyLoans(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<LoanResponse> loans = loanService.getMyLoans(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved", loans));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String loanId) {
        LoanResponse loan = loanService.getLoan(userDetails.getUsername(), loanId);
        return ResponseEntity.ok(ApiResponse.success("Loan retrieved", loan));
    }

    @PostMapping("/pay-emi")
    public ResponseEntity<ApiResponse<LoanResponse>> payEmi(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PayEmiRequest request) {
        LoanResponse response = loanService.payEmi(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("EMI paid successfully", response));
    }

    // EMI Calculator - public endpoint
    @GetMapping("/calculate-emi")
    public ResponseEntity<ApiResponse<Object>> calculateEmi(
            @RequestParam BigDecimal amount,
            @RequestParam String type,
            @RequestParam int months) {
        Loan.LoanType loanType = Loan.LoanType.valueOf(type.toUpperCase());
        BigDecimal rate = Loan.getInterestRate(loanType);
        BigDecimal emi  = Loan.calculateEmi(amount, rate, months);
        BigDecimal total = emi.multiply(new BigDecimal(months));
        BigDecimal interest = total.subtract(amount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("loanAmount",    amount);
        result.put("loanType",      loanType);
        result.put("interestRate",  rate + "% p.a.");
        result.put("tenureMonths",  months);
        result.put("emiAmount",     emi);
        result.put("totalPayable",  total.setScale(2, java.math.RoundingMode.HALF_UP));
        result.put("totalInterest", interest.setScale(2, java.math.RoundingMode.HALF_UP));
        return ResponseEntity.ok(ApiResponse.success("EMI calculated", result));
    }

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<Object>> getLoanRates() {
        Map<String, Object> rates = new LinkedHashMap<>();
        rates.put("PERSONAL",  Map.of("rate", "12.00% p.a.", "maxAmount", "₹5,00,000", "maxTenure", "60 months"));
        rates.put("HOME",      Map.of("rate", "8.50% p.a.",  "maxAmount", "₹1,00,00,000", "maxTenure", "360 months"));
        rates.put("CAR",       Map.of("rate", "9.50% p.a.",  "maxAmount", "₹20,00,000", "maxTenure", "84 months"));
        rates.put("EDUCATION", Map.of("rate", "10.00% p.a.", "maxAmount", "₹20,00,000", "maxTenure", "120 months"));
        return ResponseEntity.ok(ApiResponse.success("Loan interest rates", rates));
    }

    // Admin endpoints
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getPendingLoans() {
        return ResponseEntity.ok(ApiResponse.success("Pending loans", loanService.getPendingLoans()));
    }

    @PutMapping("/admin/{loanId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponse>> approveLoan(
            @PathVariable String loanId,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.getOrDefault("adminNotes", "") : "";
        LoanResponse response = loanService.approveLoan(loanId, notes);
        return ResponseEntity.ok(ApiResponse.success("Loan approved and disbursed", response));
    }

    @PutMapping("/admin/{loanId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponse>> rejectLoan(
            @PathVariable String loanId,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Application rejected");
        LoanResponse response = loanService.rejectLoan(loanId, reason);
        return ResponseEntity.ok(ApiResponse.success("Loan rejected", response));
    }
}
