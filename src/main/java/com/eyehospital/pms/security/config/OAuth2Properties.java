package com.eyehospital.pms.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth2 JWT token configuration properties.
 * Replaces the old {@code app.jwt.*} HMAC-based properties with RSA-backed OAuth2 settings.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2Properties {

    /** Access token lifetime in milliseconds (default 15 minutes). */
    private long accessTokenExpiryMs = 900_000;

    /** Refresh token lifetime in milliseconds (default 7 days). */
    private long refreshTokenExpiryMs = 604_800_000;

    /** Issuer URI embedded in every JWT (iss claim). */
    private String issuer = "eye-hospital-pms";
}
