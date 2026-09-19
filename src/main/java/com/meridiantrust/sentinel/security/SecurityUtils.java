package com.meridiantrust.sentinel.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Small helper to resolve the acting user for audit records. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Username of the authenticated principal, or {@code "system"} when unauthenticated. */
    public static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "system";
        }
        return auth.getName();
    }
}
