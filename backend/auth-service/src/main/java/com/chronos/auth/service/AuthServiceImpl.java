package com.chronos.auth.service;

import com.chronos.auth.dto.*;
import com.chronos.auth.entity.Organization;
import com.chronos.auth.entity.RefreshToken;
import com.chronos.auth.entity.Role;
import com.chronos.auth.entity.User;
import com.chronos.auth.exception.BadRequestException;
import com.chronos.auth.exception.TokenRefreshException;
import com.chronos.auth.repository.OrganizationRepository;
import com.chronos.auth.repository.RefreshTokenRepository;
import com.chronos.auth.repository.UserRepository;
import com.chronos.auth.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final long refreshTokenExpirationMs;

    public AuthServiceImpl(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuthenticationManager authenticationManager,
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address '" + request.getEmail() + "' is already registered");
        }

        // Find or create Organization
        boolean isNewOrganization = false;
        Optional<Organization> existingOrg = organizationRepository.findByName(request.getOrganizationName());
        Organization organization;

        if (existingOrg.isPresent()) {
            organization = existingOrg.get();
        } else {
            organization = new Organization(request.getOrganizationName());
            organization = organizationRepository.save(organization);
            isNewOrganization = true;
        }

        // Determine user role
        Role role = request.getRole();
        if (role == null) {
            role = isNewOrganization ? Role.OWNER : Role.VIEWER;
        }

        // Create User
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                role,
                organization
        );
        User savedUser = userRepository.save(user);

        // Generate Tokens
        String accessToken = tokenProvider.generateAccessToken(savedUser);
        RefreshToken refreshToken = createRefreshToken(savedUser);

        long expiresInSeconds = tokenProvider.getAccessTokenExpirationMs() / 1000;

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                expiresInSeconds,
                UserDto.fromEntity(savedUser)
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found with email: " + request.getEmail()));

        String accessToken = tokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        long expiresInSeconds = tokenProvider.getAccessTokenExpirationMs() / 1000;

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                expiresInSeconds,
                UserDto.fromEntity(user)
        );
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new TokenRefreshException("Refresh token is not present in database"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token was expired. Please make a new login request");
        }

        User user = refreshToken.getUser();
        String newAccessToken = tokenProvider.generateAccessToken(user);
        long expiresInSeconds = tokenProvider.getAccessTokenExpirationMs() / 1000;

        return new AuthResponse(
                newAccessToken,
                refreshToken.getToken(),
                expiresInSeconds,
                UserDto.fromEntity(user)
        );
    }

    private RefreshToken createRefreshToken(User user) {
        // Delete existing refresh token for user if present
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = new RefreshToken(
                user,
                UUID.randomUUID().toString(),
                Instant.now().plusMillis(refreshTokenExpirationMs)
        );

        return refreshTokenRepository.save(refreshToken);
    }
}
