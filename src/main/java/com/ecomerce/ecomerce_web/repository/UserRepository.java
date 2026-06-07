package com.ecomerce.ecomerce_web.repository;

import com.ecomerce.ecomerce_web.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.emailVerified = false AND u.createdAt < :date")
    int deleteUnverifiedUsers( @Param("date") LocalDateTime date);

}
