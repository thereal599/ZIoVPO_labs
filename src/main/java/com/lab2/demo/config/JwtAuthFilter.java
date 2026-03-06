package com.lab2.demo.config;

import com.lab2.demo.model.SessionStatus;
import com.lab2.demo.model.UserSession;
import com.lab2.demo.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final UserSessionRepository sessionRepository;

    public JwtAuthFilter(JwtTokenProvider jwt, UserSessionRepository sessionRepository) {
        this.jwt = jwt;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                // is valid + access?
                if (jwt.validate(token) && jwt.getType(token) == JwtTokenType.ACCESS) {

                    UUID sessionId = jwt.getSessionId(token);

                    UserSession session = sessionRepository.findById(sessionId)
                            .orElse(null);
                    // is active + not expired?
                    if (session != null
                            && session.getStatus() == SessionStatus.ACTIVE
                            && session.getAccessTokenExpiry() != null
                            && session.getAccessTokenExpiry().isAfter(Instant.now())
                            && token.equals(session.getAccessToken())) {

                        String username = jwt.getUsername(token);
                        String role = jwt.getRole(token);

                        var authentication = new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception ignored) {}
        }

        filterChain.doFilter(request, response);
    }
}
