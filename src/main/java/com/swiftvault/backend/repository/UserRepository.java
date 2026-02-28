package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email)    LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "u.userId          LIKE CONCAT('%', :query, '%')")
    List<User> searchUsers(@Param("query") String query);
}