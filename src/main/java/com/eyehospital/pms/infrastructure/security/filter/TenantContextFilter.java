package com.eyehospital.pms.infrastructure.security.filter;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that runs after OAuth2 Bearer-token authentication to extract the
 * {@code hospitalId} claim from the validated JWT and set it as a request attribute.
 * <p>
 * This enables Row Level Security (RLS) tenant isolation downstream.
 * Replaces the tenant-context portion of the old {@code JwtAuthenticationFilter}.
 */
@Slf4j
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String hospitalId = jwtAuth.getToken().getClaimAsString("hospitalId");
            if (hospitalId != null) {
                request.setAttribute("hospitalId", hospitalId);
                log.trace("Tenant context set: hospitalId={}", hospitalId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
