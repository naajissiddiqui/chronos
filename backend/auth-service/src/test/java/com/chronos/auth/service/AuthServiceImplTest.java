package com.chronos.auth.service;

import com.chronos.auth.dto.AuthResponse;
import com.chronos.auth.dto.LoginRequest;
import com.chronos.auth.dto.RefreshTokenRequest;
import com.chronos.auth.dto.RegisterRequest;
import com.chronos.auth.entity.Organization;
import com.chronos.auth.entity.RefreshToken;
import com.chronos.auth.entity.Role;
import com.chronos.auth.entity.User;
import com.chronos.auth.exception.TokenRefreshException;
import com.chronos.auth.repository.OrganizationRepository;
import com.chronos.auth.repository.RefreshTokenRepository;
import com.chronos.auth.repository.UserRepository;
import com.chronos.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private AuthServiceImpl authService;
    private final long refreshTokenExpirationMs = 604800000L;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                organizationRepository,
                refreshTokenRepository,
                passwordEncoder,
                tokenProvider,
                authenticationManager,
                refreshTokenExpirationMs
        );

        testOrg = new Organization("Test Org");
        testUser = new User("user@example.com", "password", "John", "Doe", Role.OWNER, testOrg);
    }

    @Test
    @DisplayName("First login creates a new refresh token for the user")
    void testFirstLogin_CreatesRefreshToken() {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(testUser)).thenReturn("access-token-1");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token-1", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository).findByUser(testUser);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedToken = refreshTokenCaptor.getValue();
        assertEquals(testUser, savedToken.getUser());
        assertEquals(response.getRefreshToken(), savedToken.getToken());
    }

    @Test
    @DisplayName("Second login for the same user updates the existing refresh token record rather than inserting a new one")
    void testSecondLogin_UpdatesExistingRefreshToken() {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "password");
        String oldTokenString = "old-refresh-token-uuid";
        Instant oldExpiryDate = Instant.now().plusSeconds(100);

        RefreshToken existingRefreshToken = new RefreshToken(testUser, oldTokenString, oldExpiryDate);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(testUser)).thenReturn("access-token-2");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.of(existingRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token-2", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals(oldTokenString, response.getRefreshToken());

        verify(refreshTokenRepository).findByUser(testUser);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedToken = refreshTokenCaptor.getValue();
        assertSame(existingRefreshToken, savedToken);
        assertEquals(response.getRefreshToken(), savedToken.getToken());
        assertTrue(savedToken.getExpiryDate().isAfter(oldExpiryDate));
    }

    @Test
    @DisplayName("Registration creates the user's first refresh token successfully")
    void testRegister_CreatesFirstRefreshToken() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Doe");
        registerRequest.setOrganizationName("Acme Corp");

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(organizationRepository.findByName("Acme Corp")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any(User.class))).thenReturn("register-access-token");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.findByUser(any(User.class))).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("register-access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh token validation with a valid token returns new access token")
    void testRefreshToken_ValidToken() {
        String tokenStr = UUID.randomUUID().toString();
        RefreshToken validToken = new RefreshToken(testUser, tokenStr, Instant.now().plusSeconds(3600));
        RefreshTokenRequest request = new RefreshTokenRequest(tokenStr);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(validToken));
        when(tokenProvider.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals(tokenStr, response.getRefreshToken());
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Expired refresh token throws TokenRefreshException and deletes the token")
    void testRefreshToken_ExpiredToken() {
        String tokenStr = UUID.randomUUID().toString();
        RefreshToken expiredToken = new RefreshToken(testUser, tokenStr, Instant.now().minusSeconds(3600));
        RefreshTokenRequest request = new RefreshTokenRequest(tokenStr);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(expiredToken));

        TokenRefreshException exception = assertThrows(
                TokenRefreshException.class,
                () -> authService.refreshToken(request)
        );

        assertTrue(exception.getMessage().contains("Refresh token was expired"));
        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    @DisplayName("Invalid/non-existent refresh token throws TokenRefreshException")
    void testRefreshToken_InvalidToken() {
        String tokenStr = "non-existent-token";
        RefreshTokenRequest request = new RefreshTokenRequest(tokenStr);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.empty());

        TokenRefreshException exception = assertThrows(
                TokenRefreshException.class,
                () -> authService.refreshToken(request)
        );

        assertTrue(exception.getMessage().contains("Refresh token is not present in database"));
        verify(refreshTokenRepository, never()).delete(any());
    }
}
