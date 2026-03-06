package com.lab2.demo.repository;

import com.lab2.demo.model.SessionStatus;
import com.lab2.demo.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshToken(String refreshToken);
    Optional<UserSession> findByIdAndStatus(UUID id, SessionStatus status);
}
