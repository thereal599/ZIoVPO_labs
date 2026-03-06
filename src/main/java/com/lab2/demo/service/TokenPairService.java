package com.lab2.demo.service;

import com.lab2.demo.dto.LoginRequest;
import com.lab2.demo.dto.RefreshRequest;
import com.lab2.demo.dto.TokenPairResponse;
import com.lab2.demo.model.AppUser;
import com.lab2.demo.model.SessionStatus;
import com.lab2.demo.model.UserSession;
import com.lab2.demo.repository.AppUserRepository;
import com.lab2.demo.repository.UserSessionRepository;
import com.lab2.demo.config.JwtTokenProvider;
import com.lab2.demo.config.JwtTokenType;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TokenPairService {

    private final AppUserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;

    public TokenPairService(AppUserRepository userRepository,
                            UserSessionRepository sessionRepository,
                            PasswordEncoder passwordEncoder,
                            JwtTokenProvider jwt) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    public TokenPairResponse login(LoginRequest req) {
        AppUser user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Bad credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Bad credentials");
        }

        UserSession session = UserSession.builder()
                .username(user.getUsername())
                .deviceId(req.getDeviceId())
                .status(SessionStatus.ACTIVE)
                .build();
        session = sessionRepository.save(session);

        String access = jwt.generateAccessToken(user.getUsername(), user.getRole().name(), session.getId());
        String refresh = jwt.generateRefreshToken(user.getUsername(), session.getId());

        session.setAccessToken(access);
        session.setRefreshToken(refresh);
        session.setAccessTokenExpiry(jwt.getExpiry(access));
        session.setRefreshTokenExpiry(jwt.getExpiry(refresh));
        sessionRepository.save(session);

        return new TokenPairResponse(access, refresh);
    }

    public TokenPairResponse refresh(RefreshRequest req) {
        String oldRefresh = req.getRefreshToken();

        try {
            if (!jwt.validate(oldRefresh) || jwt.getType(oldRefresh) != JwtTokenType.REFRESH) {
                throw new IllegalArgumentException("Invalid refresh token");
            }
        } catch (JwtException ex) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        UserSession oldSession = sessionRepository.findByRefreshToken(oldRefresh)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!oldSession.getDeviceId().equals(req.getDeviceId())) {
            throw new IllegalArgumentException("Device mismatch");
        }

        if (oldSession.getStatus() == SessionStatus.USED) {
            oldSession.setStatus(SessionStatus.REVOKED);
            sessionRepository.save(oldSession);
            throw new IllegalArgumentException("Refresh token reuse detected");
        }
        if (oldSession.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalArgumentException("Session is not active");
        }
        if (oldSession.getRefreshTokenExpiry().isBefore(Instant.now())) {
            oldSession.setStatus(SessionStatus.USED); // можно и REVOKED, но по логике — уже отработал
            sessionRepository.save(oldSession);
            throw new IllegalArgumentException("Refresh token expired");
        }

        oldSession.setStatus(SessionStatus.USED);
        sessionRepository.save(oldSession);

        UUID newSessionId = UUID.randomUUID();
        UserSession newSession = UserSession.builder()
                .username(oldSession.getUsername())
                .deviceId(oldSession.getDeviceId())
                .status(SessionStatus.ACTIVE)
                .build();
        newSession = sessionRepository.save(newSession);

        AppUser user = userRepository.findByUsername(oldSession.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newAccess = jwt.generateAccessToken(user.getUsername(), user.getRole().name(), newSession.getId());
        String newRefresh = jwt.generateRefreshToken(user.getUsername(), newSession.getId());

        newSession.setAccessToken(newAccess);
        newSession.setRefreshToken(newRefresh);
        newSession.setAccessTokenExpiry(jwt.getExpiry(newAccess));
        newSession.setRefreshTokenExpiry(jwt.getExpiry(newRefresh));
        sessionRepository.save(newSession);

        return new TokenPairResponse(newAccess, newRefresh);
    }
}
