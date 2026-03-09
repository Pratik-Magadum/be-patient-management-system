package com.eyehospital.pms.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * OAuth2 JWT configuration — provides RSA-based {@link JwtEncoder} and {@link JwtDecoder}.
 * <p>
 * The RSA key pair is generated in-memory at startup. For production deployments,
 * replace {@link #rsaKeyPair()} with a bean that loads keys from a secure store
 * (e.g., PKCS#12 keystore, Vault, cloud KMS).
 */
@Slf4j
@Configuration
public class OAuth2Config {

    /**
     * Generates a 2048-bit RSA key pair at application startup.
     * This is suitable for development. In production, load keys from a persistent store.
     */
    @Bean
    public KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            log.info("RSA key pair generated for OAuth2 JWT signing (2048-bit)");
            return keyPair;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * JWK source backed by the generated RSA key pair — used by {@link NimbusJwtEncoder}.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair rsaKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /**
     * Standard OAuth2 JWT encoder (Nimbus) using RSA-256 signing.
     */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Standard OAuth2 JWT decoder (Nimbus) using the RSA public key for verification.
     */
    @Bean
    public JwtDecoder jwtDecoder(KeyPair rsaKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
