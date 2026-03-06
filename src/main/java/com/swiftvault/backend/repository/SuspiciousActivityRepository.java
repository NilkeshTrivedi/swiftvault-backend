// ═══════════════════════════════════════════════════════════════════════════
// FILE 1: SuspiciousActivityRepository.java
// Path: src/main/java/com/swiftvault/backend/repository/
// ═══════════════════════════════════════════════════════════════════════════
package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.SuspiciousActivity;
import com.swiftvault.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SuspiciousActivityRepository extends JpaRepository<SuspiciousActivity, String> {

    List<SuspiciousActivity> findByUserOrderByCreatedAtDesc(User user);

    Page<SuspiciousActivity> findByStatusOrderByCreatedAtDesc(
            SuspiciousActivity.AlertStatus status, Pageable pageable);

    Page<SuspiciousActivity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(SuspiciousActivity.AlertStatus status);

    @Query("SELECT s FROM SuspiciousActivity s WHERE s.user = :user " +
            "AND s.alertType = :type AND s.createdAt >= :since")
    List<SuspiciousActivity> findRecentByUserAndType(
            @Param("user") User user,
            @Param("type") SuspiciousActivity.AlertType type,
            @Param("since") LocalDateTime since);
}