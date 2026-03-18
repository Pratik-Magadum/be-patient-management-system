package com.eyehospital.pms.infrastructure.security.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyehospital.pms.common.exception.BusinessException;
import com.eyehospital.pms.common.exception.ResourceNotFoundException;
import com.eyehospital.pms.infrastructure.security.dto.AuthResponseDto;
import com.eyehospital.pms.infrastructure.security.dto.LoginRequestDto;
import com.eyehospital.pms.infrastructure.security.dto.RefreshTokenRequestDto;
import com.eyehospital.pms.infrastructure.security.entity.RefreshToken;
import com.eyehospital.pms.infrastructure.security.entity.User;
import com.eyehospital.pms.infrastructure.security.repository.RefreshTokenRepository;
import com.eyehospital.pms.infrastructure.security.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticates user and returns both access + refresh token.
     * The `expiresIn` field tells the frontend when to call /auth/refresh.
     */
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByHospitalIdAndUsernameAndActiveTrue(
                        request.getHospitalId(), request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid username or password");
        }

        String accessToken = jwtService.generateAccessToken(
                user.getUserId(), user.getUsername(), user.getHospitalId(), user.getRole());

        String rawRefreshToken = jwtService.generateRefreshToken(user.getUserId(), user.getUsername());

        // Revoke old refresh tokens for this user (single session policy)
        refreshTokenRepository.deleteByUserId(user.getUserId());

        // Persist new refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getUserId());
        refreshToken.setToken(rawRefreshToken);
        refreshToken.setExpiresAt(Instant.now().plusMillis(
                jwtService.getAccessTokenExpiryMs() * 336)); // ~7 days
        refreshTokenRepository.save(refreshToken);

        log.info("User '{}' logged in successfully", user.getUsername());

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiryMs()) // ← Frontend uses this!
                .role(user.getRole())
                .hospitalId(user.getHospitalId().toString())
                .build();
    }

    /**
     * Validates refresh token and issues a new access token.
     * Called by frontend when access token is expired (401 received).
     */
    @Transactional
    public AuthResponseDto refreshToken(RefreshTokenRequestDto request) {
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Invalid or revoked refresh token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token expired. Please login again.");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", storedToken.getUserId().toString()));

        String newAccessToken = jwtService.generateAccessToken(
                user.getUserId(), user.getUsername(), user.getHospitalId(), user.getRole());

        return AuthResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken()) // reuse same refresh token
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiryMs())
                .role(user.getRole())
                .hospitalId(user.getHospitalId().toString())
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }
}
