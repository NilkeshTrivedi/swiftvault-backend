package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.SavingsGoal;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, String> {
    List<SavingsGoal> findByUser(User user);
    List<SavingsGoal> findByUserAndStatus(User user, SavingsGoal.GoalStatus status);
}