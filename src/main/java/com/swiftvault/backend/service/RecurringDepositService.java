package com.swiftvault.backend.service;

import com.swiftvault.backend.dto.request.OpenRdRequest;
import com.swiftvault.backend.dto.response.RdResponse;
import java.util.List;

public interface RecurringDepositService {
    RdResponse openRd(String userId, OpenRdRequest request);
    List<RdResponse> getMyRds(String userId);
    RdResponse getRd(String userId, String rdId);
    RdResponse payInstallment(String userId, String rdId, String transactionPin);
    RdResponse closeRd(String userId, String rdId, String transactionPin);
    void processAutoDebitRds(); // Scheduled job
}
