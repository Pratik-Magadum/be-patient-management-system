package com.eyehospital.pms.security.controller;
import com.eyehospital.pms.common.response.ApiResponse;
import com.eyehospital.pms.security.dto.AuthResponseDto;
import com.eyehospital.pms.security.dto.LoginRequestDto;
import com.eyehospital.pms.security.dto.RefreshTokenRequestDto;
import com.eyehospital.pms.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, Refresh Token, Logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Returns accessToken + refreshToken + expiresIn")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Login successful", response));
    }

    /**
     * Called by frontend when it receives 401 TOKEN_EXPIRED.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token",
               description = "Exchange a valid refresh token for a new access token")
    public ResponseEntity<ApiResponse<AuthResponseDto>> refresh(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        AuthResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Logged out successfully", null));
    }
}
