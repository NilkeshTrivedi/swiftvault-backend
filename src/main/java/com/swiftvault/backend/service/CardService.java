package com.swiftvault.backend.service;

import com.swiftvault.backend.dto.request.CardLimitRequest;
import com.swiftvault.backend.dto.request.CardRequest;
import com.swiftvault.backend.dto.response.CardResponse;
import java.util.List;

public interface CardService {
    CardResponse issueCard(String userId, CardRequest request);
    List<CardResponse> getMyCards(String userId);
    CardResponse getCard(String userId, String cardId);
    CardResponse freezeCard(String userId, String cardId);
    CardResponse unfreezeCard(String userId, String cardId);
    CardResponse blockCard(String userId, String cardId);
    CardResponse updateLimits(String userId, CardLimitRequest request);
    CardResponse toggleFeature(String userId, String cardId, String feature, boolean enabled);
}
