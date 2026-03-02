package com.swiftvault.backend.service;

import com.swiftvault.backend.dto.request.LoanApplicationRequest;
import com.swiftvault.backend.dto.request.PayEmiRequest;
import com.swiftvault.backend.dto.response.LoanResponse;
import java.util.List;

public interface LoanService {
    LoanResponse applyForLoan(String userId, LoanApplicationRequest request);
    List<LoanResponse> getMyLoans(String userId);
    LoanResponse getLoan(String userId, String loanId);
    LoanResponse payEmi(String userId, PayEmiRequest request);
    // Admin
    List<LoanResponse> getPendingLoans();
    LoanResponse approveLoan(String loanId, String adminNotes);
    LoanResponse rejectLoan(String loanId, String reason);
}
