package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM User u
        WHERE u.email = :email
        """)
    Optional<User> findByEmailForUpdate(
            @Param("email")
            String email
    );
}