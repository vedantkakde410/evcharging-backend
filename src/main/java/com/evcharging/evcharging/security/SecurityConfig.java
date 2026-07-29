package com.evcharging.evcharging.security;

import com.evcharging.evcharging.dto.ApiErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

// Module 9: every path rule below is a deliberate authorization decision,
// not a default - see AUTHENTICATION_DESIGN.md section 13 Risk 4 (which
// this module resolves) and CHANGELOG.md Module 9 for the full rationale
// per endpoint.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Bearer-token auth, not cookies - CSRF protection exists to stop a
            // browser from auto-attaching credentials to a forged request, which
            // doesn't apply here (ARCHITECTURE_DECISIONS.md section 8).
            .csrf(AbstractHttpConfigurer::disable)
            // Delegates to the existing WebMvcConfigurer CORS mapping in
            // EvchargingApplication - not redefined here, so the two don't drift.
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health").permitAll()
                // More specific than the /auth/** rule below and must be
                // declared first - first-match-wins. Everything else under
                // /auth is public (registration/login/OTP/reset can't
                // require a token to reach), but logging out obviously
                // requires one to know which token to revoke.
                .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                .requestMatchers("/auth/**").permitAll()
                // Public browsing - no account needed to see what's available,
                // matches this app's marketplace nature (Home/Station pages
                // are reachable logged-out per COMPONENT_ARCHITECTURE.md).
                .requestMatchers(HttpMethod.GET,
                        "/api/stations", "/api/stations/*/chargers", "/api/stations/*/reviews")
                        .permitAll()
                // Writing a review and viewing booking history both require
                // being a real, logged-in user - StationController/
                // BookingController enforce per-resource ownership on top of
                // this (see SecurityUtils / Module 9 CHANGELOG entry).
                .requestMatchers(HttpMethod.POST, "/api/stations/*/review").authenticated()
                .requestMatchers("/api/users/*/bookings").authenticated()
                // Owner-only surface. Listed explicitly rather than relying on
                // the RoleHierarchy bean below to auto-apply within
                // authorizeHttpRequests (that wiring is auto-configured in
                // recent Spring Security versions, but an explicit
                // hasAnyRole here doesn't depend on it holding true).
                .requestMatchers("/owner/**").hasAnyRole("OWNER", "ADMIN")
                // Fail closed: anything not explicitly listed above requires
                // at least a valid, authenticated session rather than
                // defaulting to open.
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                "UNAUTHORIZED", "Authentication required."))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                                "FORBIDDEN", "You do not have permission to access this resource."))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    private void writeJsonError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ApiErrorResponse(error, message)));
    }

    // Documents the intended role hierarchy (ADMIN implies OWNER and USER)
    // as a source of truth and covers any future @PreAuthorize method
    // security, which auto-detects a RoleHierarchy bean reliably. The
    // path-based rules above are written explicitly and don't depend on
    // this being auto-applied there too.
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("OWNER")
                .role("ADMIN").implies("USER")
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
