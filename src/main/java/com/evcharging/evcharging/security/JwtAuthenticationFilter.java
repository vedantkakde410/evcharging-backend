package com.evcharging.evcharging.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Validates the Authorization: Bearer <jwt> header on every request and
// populates the SecurityContext when it's valid. Does not itself reject
// requests with a missing/invalid token - SecurityConfig's per-path rules
// (now real as of Module 9, see AUTHENTICATION_DESIGN.md section 13 Risk 4
// and CHANGELOG.md Module 9) decide what that means for a given endpoint.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    public JwtAuthenticationFilter(JwtService jwtService, TokenRevocationService tokenRevocationService) {
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                Claims claims = jwtService.parseClaims(token);

                // A password-reset token must never authenticate a request as a
                // logged-in user - it has no role claim and is only meaningful to
                // /auth/reset-password, which reads it explicitly, not via this filter.
                // A revoked (logged-out) token is likewise left unauthenticated rather
                // than rejected here - same reasoning as an expired/malformed one.
                if (claims.get("purpose") == null && !tokenRevocationService.isRevoked(claims.getId())) {
                    String role = String.valueOf(claims.get("role"));
                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    var authentication = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // Invalid/expired/malformed - leave the request unauthenticated.
            }
        }

        filterChain.doFilter(request, response);
    }
}
