package com.evcharging.evcharging.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

// Programmatic per-resource ownership checks for controllers where the
// resource id is a path variable (e.g. /api/users/{id}/bookings) - a role
// check alone (Module 9's SecurityConfig) only proves "this is *some*
// logged-in user", not "this is the *right* user". Throwing
// AccessDeniedException here is caught by Spring Security's
// ExceptionTranslationFilter the same way a declarative authorizeHttpRequests
// failure would be, so it produces the same JSON 403 body.
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // Admins bypass ownership checks by design (they're the "future admin
    // APIs" surface this module is explicitly asked to already support).
    public static void assertOwnResource(Authentication authentication, long resourceOwnerId) {
        if (isAdmin(authentication)) {
            return;
        }

        if (!currentUserId(authentication).equals(resourceOwnerId)) {
            throw new AccessDeniedException("Not authorized to access this resource");
        }
    }
}
