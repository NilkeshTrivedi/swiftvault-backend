package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository — Spring Data JPA writes all SQL automatically.
 *
 * JpaRepository<User, String> gives you for free:
 *   save(), findById(), findAll(), delete(), count(), existsById()...
 *
 * Method name conventions Spring understands:
 *   findBy{Field}          → SELECT * FROM users WHERE field = ?
 *   findBy{Field}Containing → SELECT * FROM users WHERE field LIKE %?%
 *   existsBy{Field}        → SELECT COUNT(*) > 0 WHERE field = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Custom JPQL query — searches across multiple fields
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email)    LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "u.userId          LIKE CONCAT('%', :query, '%')")
    List<User> searchUsers(String query);
}