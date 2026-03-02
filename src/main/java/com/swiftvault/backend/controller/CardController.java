package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.CardLimitRequest;
import com.swiftvault.backend.dto.request.CardRequest;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.CardResponse;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    // FIX #2: Changed @AuthenticationPrincipal from UserDetails to User entity
    // (consistent with all other controllers, and required because CardService.findById expects userId, not email)
    // UserDetails.getUsername() returns email — but UserService.findById() expects userId → causes "User not found" errors

    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<CardResponse>> issueCard(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CardRequest request) {
        CardResponse response = cardService.issueCard(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Virtual card issued successfully. Save your CVV — it won't be shown again.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> getMyCards(
            @AuthenticationPrincipal User user) {
        List<CardResponse> cards = cardService.getMyCards(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Cards retrieved", cards));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<ApiResponse<CardResponse>> getCard(
            @AuthenticationPrincipal User user,
            @PathVariable String cardId) {
        CardResponse card = cardService.getCard(user.getUserId(), cardId);
        return ResponseEntity.ok(ApiResponse.success("Card retrieved", card));
    }

    @PutMapping("/{cardId}/freeze")
    public ResponseEntity<ApiResponse<CardResponse>> freezeCard(
            @AuthenticationPrincipal User user,
            @PathVariable String cardId) {
        CardResponse card = cardService.freezeCard(user.getUserId(), cardId);
        return ResponseEntity.ok(ApiResponse.success("Card frozen successfully", card));
    }

    @PutMapping("/{cardId}/unfreeze")
    public ResponseEntity<ApiResponse<CardResponse>> unfreezeCard(
            @AuthenticationPrincipal User user,
            @PathVariable String cardId) {
        CardResponse card = cardService.unfreezeCard(user.getUserId(), cardId);
        return ResponseEntity.ok(ApiResponse.success("Card unfrozen successfully", card));
    }

    @PutMapping("/{cardId}/block")
    public ResponseEntity<ApiResponse<CardResponse>> blockCard(
            @AuthenticationPrincipal User user,
            @PathVariable String cardId) {
        CardResponse card = cardService.blockCard(user.getUserId(), cardId);
        return ResponseEntity.ok(ApiResponse.success("Card permanently blocked", card));
    }

    @PutMapping("/limits")
    public ResponseEntity<ApiResponse<CardResponse>> updateLimits(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CardLimitRequest request) {
        CardResponse card = cardService.updateLimits(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Card limits updated", card));
    }

    @PutMapping("/{cardId}/toggle/{feature}")
    public ResponseEntity<ApiResponse<CardResponse>> toggleFeature(
            @AuthenticationPrincipal User user,
            @PathVariable String cardId,
            @PathVariable String feature,
            @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        CardResponse card = cardService.toggleFeature(user.getUserId(), cardId, feature, enabled);
        return ResponseEntity.ok(ApiResponse.success("Card feature updated", card));
    }
}