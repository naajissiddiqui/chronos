package com.chronos.auth;

import com.chronos.auth.dto.AuthResponse;
import com.chronos.auth.dto.LoginRequest;
import com.chronos.auth.dto.RefreshTokenRequest;
import com.chronos.auth.dto.RegisterRequest;
import com.chronos.auth.entity.RefreshToken;
import com.chronos.auth.repository.RefreshTokenRepository;
import com.chronos.auth.repository.UserRepository;
import com.chronos.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Multiple logins for the same user succeed without unique constraint violation")
    void testMultipleLogins_SameUser() {
        // 1. Register a user
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail("integration@example.com");
        registerReq.setPassword("Password123!");
        registerReq.setFirstName("Alice");
        registerReq.setLastName("Smith");
        registerReq.setOrganizationName("Acme Testing");

        AuthResponse regResponse = authService.register(registerReq);
        assertNotNull(regResponse);
        assertNotNull(regResponse.getRefreshToken());

        assertEquals(1, refreshTokenRepository.count());

        // 2. First Login
        LoginRequest loginReq = new LoginRequest("integration@example.com", "Password123!");
        AuthResponse login1Response = authService.login(loginReq);
        assertNotNull(login1Response);
        assertNotNull(login1Response.getRefreshToken());

        // Exactly one refresh token should exist for the user
        assertEquals(1, refreshTokenRepository.count());

        // 3. Second Login for the SAME user (previously failed with duplicate key error)
        AuthResponse login2Response = authService.login(loginReq);
        assertNotNull(login2Response);
        assertNotNull(login2Response.getRefreshToken());

        // Must still be exactly one refresh token in DB
        assertEquals(1, refreshTokenRepository.count());
        assertNotEquals(login1Response.getRefreshToken(), login2Response.getRefreshToken());
    }

    @Test
    @DisplayName("Refresh token validation and expiration in database")
    void testRefreshTokenValidationAndExpiry() {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail("refreshtest@example.com");
        registerReq.setPassword("Password123!");
        registerReq.setFirstName("Bob");
        registerReq.setLastName("Jones");
        registerReq.setOrganizationName("Test Org");

        AuthResponse regResponse = authService.register(registerReq);
        String refreshTokenStr = regResponse.getRefreshToken();

        // Validate refresh token
        AuthResponse refreshed = authService.refreshToken(new RefreshTokenRequest(refreshTokenStr));
        assertNotNull(refreshed.getAccessToken());

        // Manually expire token in DB
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(refreshTokenStr);
        assertTrue(tokenOpt.isPresent());
        RefreshToken token = tokenOpt.get();
        token.setExpiryDate(Instant.now().minusSeconds(3600));
        refreshTokenRepository.save(token);

        // Attempting to refresh expired token throws exception and removes token from DB
        assertThrows(RuntimeException.class, () -> authService.refreshToken(new RefreshTokenRequest(refreshTokenStr)));
        assertFalse(refreshTokenRepository.findByToken(refreshTokenStr).isPresent());
    }
}
