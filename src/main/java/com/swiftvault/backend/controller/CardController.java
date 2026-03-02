package com.swiftvault.backend.controller;

import com.swiftvault.backend.dto.request.CardLimitRequest;
import com.swiftvault.backend.dto.request.CardRequest;
import com.swiftvault.backend.dto.response.ApiResponse;
import com.swiftvault.backend.dto.response.CardResponse;
import com.swiftvault.backend.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<CardResponse>> issueCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CardRequest request) {
        CardResponse response = cardService.issueCard(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Virtual card issued successfully. Save your CVV — it won't be shown again.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> getMyCards(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CardResponse> cards = cardService.getMyCards(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cards retrieved", cards));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<ApiResponse<CardResponse>> getCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardId) {
        CardResponse card = cardService.getCard(userDetails.getUsername(), cardId);
        return ResponseEntity.ok(ApiResponse.success("Card retrieved", card));
    }

    @PutMapping("/{cardId}/freeze")
    public ResponseEntity<ApiResponse<CardResponse>> freezeCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardId) {
        CardResponse card = cardService.freezeCard(userDetails.getUsername(), cardId);
        return ResponseEntity.ok(ApiResponse.success("Card frozen successfully", card));
    }

    @PutMapping("/{cardId}/unfreeze")
    public ResponseEntity<ApiResponse<CardResponse>> unfreezeCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardId) {
        CardResponse card = cardService.unfreezeCard(userDetails.getUsername(), cardId);
        return ResponseEntity.ok(ApiResponse.success("Card unfrozen successfully", card));
    }

    @PutMapping("/{cardId}/block")
    public ResponseEntity<ApiResponse<CardResponse>> blockCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardId) {
        CardResponse card = cardService.blockCard(userDetails.getUsername(), cardId);
        return ResponseEntity.ok(ApiResponse.success("Card permanently blocked", card));
    }

    @PutMapping("/limits")
    public ResponseEntity<ApiResponse<CardResponse>> updateLimits(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CardLimitRequest request) {
        CardResponse card = cardService.updateLimits(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Card limits updated", card));
    }

    @PutMapping("/{cardId}/toggle/{feature}")
    public ResponseEntity<ApiResponse<CardResponse>> toggleFeature(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cardId,
            @PathVariable String feature,
            @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        CardResponse card = cardService.toggleFeature(userDetails.getUsername(), cardId, feature, enabled);
        return ResponseEntity.ok(ApiResponse.success("Card feature updated", card));
    }
}
