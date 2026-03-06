package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.AutoSavingsRule;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutoSavingsRuleRepository extends JpaRepository<AutoSavingsRule, String> {
    List<AutoSavingsRule> findByUser(User user);
    List<AutoSavingsRule> findByUserAndActiveTrue(User user);
    // Get all active ROUND_UP rules — called from AccountServiceImpl on every withdrawal
    List<AutoSavingsRule> findBySourceAccountAccountNumberAndActiveTrueAndRuleType(
            String accountNumber, AutoSavingsRule.RuleType ruleType);
    // Get all active RECURRING rules — called from ScheduledJobs on 1st of month
    List<AutoSavingsRule> findByActiveTrueAndRuleType(AutoSavingsRule.RuleType ruleType);
}