package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.Referral;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, String> {
    Optional<Referral> findByReferralCode(String referralCode);
    List<Referral> findByReferrerOrderByCreatedAtDesc(User referrer);
    Optional<Referral> findByReferred(User referred);
    boolean existsByReferralCode(String code);
    long countByReferrerAndStatus(User referrer, Referral.ReferralStatus status);
}