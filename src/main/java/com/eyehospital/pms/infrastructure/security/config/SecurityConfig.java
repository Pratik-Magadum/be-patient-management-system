package com.eyehospital.pms.infrastructure.security.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static com.eyehospital.pms.common.constants.ApiConstants.APPOINTMENTS;
import static com.eyehospital.pms.common.constants.ApiConstants.AUTH_LOGIN;
import static com.eyehospital.pms.common.constants.ApiConstants.AUTH_LOGOUT;
import static com.eyehospital.pms.common.constants.ApiConstants.AUTH_REFRESH;
import static com.eyehospital.pms.common.constants.ApiConstants.CONSULTATIONS;
import static com.eyehospital.pms.common.constants.ApiConstants.DIAGNOSTICS;
import static com.eyehospital.pms.common.constants.ApiConstants.FEATURES;
import static com.eyehospital.pms.common.constants.ApiConstants.HOSPITALS;
import static com.eyehospital.pms.common.constants.ApiConstants.PATIENTS;
import com.eyehospital.pms.infrastructure.security.filter.TenantContextFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration — stateless JWT-based authentication with
 * OAuth2 Resource Server, CORS, and role-based access control.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String WILDCARD = "/**";

    // Swagger / OpenAPI endpoints
    private static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui/**", "/swagger-ui.html",
            "/v3/api-docs/**", "/v3/api-docs.yaml",
            "/swagger-resources/**", "/webjars/**"
    };

    // Role constants
    private static final String ROLE_ADMIN        = "ADMIN";
    private static final String ROLE_RECEPTIONIST = "RECEPTIONIST";
    private static final String ROLE_DOCTOR       = "DOCTOR";
    private static final String ROLE_ASSISTANT    = "ASSISTANT";

    private final TenantContextFilter tenantContextFilter;

    @Value("${app.cors.allowed-origin-patterns}")
    private List<String> allowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, AUTH_LOGIN, AUTH_REFRESH, AUTH_LOGOUT).permitAll()
                        .requestMatchers(HttpMethod.GET, HOSPITALS + WILDCARD).permitAll()
                        // Swagger / OpenAPI endpoints
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()

                        // Role-based access aligned with project's roles (README §10)
                        .requestMatchers(FEATURES)
                            .hasAnyRole(ROLE_ADMIN, ROLE_RECEPTIONIST, ROLE_DOCTOR, ROLE_ASSISTANT)
                        .requestMatchers(PATIENTS + WILDCARD)
                            .hasAnyRole(ROLE_ADMIN, ROLE_RECEPTIONIST, ROLE_DOCTOR, ROLE_ASSISTANT)
                        .requestMatchers(APPOINTMENTS + WILDCARD)
                            .hasAnyRole(ROLE_ADMIN, ROLE_RECEPTIONIST, ROLE_DOCTOR)
                        .requestMatchers(CONSULTATIONS + WILDCARD)
                            .hasAnyRole(ROLE_ADMIN, ROLE_DOCTOR)
                        .requestMatchers(DIAGNOSTICS + WILDCARD)
                            .hasAnyRole(ROLE_ADMIN, ROLE_ASSISTANT, ROLE_DOCTOR)

                        .anyRequest().authenticated()
                )
                // OAuth2 Resource Server — validates Bearer JWT tokens automatically
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"message\":\"Unauthorized\",\"errorCode\":\"UNAUTHORIZED\"}"
                            );
                        })
                )
                // Tenant context filter — extracts hospitalId from JWT and sets it as a request attribute
                .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    /**
     * Converts the {@code role} claim from our JWT into a Spring Security {@code ROLE_*} authority.
     * This replaces the manual role extraction that was done in the old JwtAuthenticationFilter.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // matches existing seed data bcrypt strength
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Hospital-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
