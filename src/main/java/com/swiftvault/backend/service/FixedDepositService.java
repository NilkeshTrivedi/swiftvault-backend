package com.swiftvault.backend.service;

import com.swiftvault.backend.dto.request.FdWithdrawRequest;
import com.swiftvault.backend.dto.request.OpenFdRequest;
import com.swiftvault.backend.dto.response.FdResponse;
import java.util.List;

public interface FixedDepositService {
    FdResponse openFd(String userId, OpenFdRequest request);
    List<FdResponse> getMyFds(String userId);
    FdResponse getFd(String userId, String fdId);
    FdResponse withdrawFd(String userId, FdWithdrawRequest request);
    void processMaturedFds(); // Scheduled job
}
