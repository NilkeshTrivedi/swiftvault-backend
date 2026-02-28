package com.swiftvault.backend.service;

import com.swiftvault.backend.dto.request.*;
import com.swiftvault.backend.dto.response.AccountResponse;
import com.swiftvault.backend.dto.response.DashboardResponse;
import com.swiftvault.backend.dto.response.TransactionResponse;

import java.util.List;

public interface AccountService {
    AccountResponse openAccount(String userId, OpenAccountRequest request);
    List<AccountResponse> getMyAccounts(String userId);
    AccountResponse getAccount(String userId, String accountNumber);
    void setNickname(String userId, String accountNumber, String nickname);

    TransactionResponse deposit(String userId, DepositRequest request);
    TransactionResponse withdraw(String userId, WithdrawRequest request);
    TransactionResponse transfer(String userId, TransferRequest request);

    List<TransactionResponse> getTransactionHistory(String userId, String accountNumber);
    List<TransactionResponse> getMiniStatement(String userId, String accountNumber);
    List<TransactionResponse> getFilteredHistory(String userId, String accountNumber, String type);

    // Admin
    List<AccountResponse> getAllAccounts();
    void freezeAccount(String accountNumber);
    void unfreezeAccount(String accountNumber);
    void closeAccount(String accountNumber);
    void applyMonthlyInterest();
    void applyLowBalanceFees();
    DashboardResponse getDashboard();
    void exportTransactionsCsv(String filePath);
}