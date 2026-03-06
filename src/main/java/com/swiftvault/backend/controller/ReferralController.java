package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.ReferralService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/referral")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    /**
     * Get the user's referral code, stats, and history.
     * Example response:
     * {
     *   "myReferralCode": "SWIFTNIL7823",
     *   "totalReferrals": 3,
     *   "completedReferrals": 2,
     *   "pendingReferrals": 1,
     *   "totalEarned": 400.00,
     *   "rewardPerReferral": "₹200 for you + ₹200 for your friend",
     *   "referrals": [...]
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyReferralInfo(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Referral info",
                referralService.getMyReferralInfo(user.getUserId(), user)));
    }
}