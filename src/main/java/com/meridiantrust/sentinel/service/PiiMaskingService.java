/*
 * Sentinel AML — Real-Time Money Laundering Detection Platform
 * Copyright (C) 2026 Chandan Kumar Shaw <shawchandankumar20@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
