package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.VirtualCard;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VirtualCardRepository extends JpaRepository<VirtualCard, String> {
    List<VirtualCard> findByUser(User user);
    List<VirtualCard> findByUserAndStatus(User user, VirtualCard.CardStatus status);
    Optional<VirtualCard> findByCardNumber(String cardNumber);
    boolean existsByCardNumber(String cardNumber);
    long countByUser(User user);
}
