package com.evcharging.evcharging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// UserDetailsServiceAutoConfiguration is excluded because this app never
// uses Spring Security's AuthenticationManager/UserDetailsService - all
// authentication is manual JWT validation in JwtAuthenticationFilter. Left
// enabled, it silently creates a default in-memory user with a random
// generated password logged on every startup - leftover scaffolding from
// Spring Security's demo/getting-started defaults, not something this app
// relies on (Module 9 cleanup, AUTHENTICATION_DESIGN.md "remove any
// remaining demo authentication logic").
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class EvchargingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvchargingApplication.class, args);
    }

    // Reads app.cors.allowed-origins (CORS_ALLOWED_ORIGINS env var, Module
    // P0) instead of a hardcoded "*" - comma-separated, e.g.
    // "https://evcharge.example.com,https://staging.evcharge.example.com".
    // Defaults to "*" so local dev/existing behavior is unchanged.
    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${app.cors.allowed-origins:*}") String allowedOrigins) {
        String[] origins = allowedOrigins.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(origins)
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }
        };
    }
}