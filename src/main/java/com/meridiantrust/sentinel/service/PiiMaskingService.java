package com.meridiantrust.sentinel.service;

import org.springframework.stereotype.Service;

/**
 * Masks sensitive PII for list views (Business Rule 8). Full values are only
 * exposed in detail views to authorized roles, enforced at the controller layer.
 */
@Service
public class PiiMaskingService {

    /** e.g. "John Michael Doe" -> "J*** M*** D**". Null-safe. */
    public String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String[] tokens = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String tok = tokens[i];
            sb.append(tok.charAt(0)).append("*".repeat(Math.max(1, tok.length() - 1)));
        }
        return sb.toString();
    }

    /** e.g. "AADH1234567" -> "********567". Null-safe. */
    public String maskId(String id) {
        if (id == null || id.isBlank()) {
            return id;
        }
        int visible = Math.min(3, id.length());
        int hidden = id.length() - visible;
        return "*".repeat(hidden) + id.substring(hidden);
    }
}
