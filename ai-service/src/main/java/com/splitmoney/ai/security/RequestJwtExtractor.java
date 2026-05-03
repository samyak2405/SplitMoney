package com.splitmoney.ai.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RequestJwtExtractor {

    private final JwtTokenService jwtTokenService;

    public record UserPrincipal(UUID userId, String email) {}

    public UserPrincipal extract(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        try {
            Claims claims = jwtTokenService.parseToken(token);
            String sub = claims.getSubject();
            String email = claims.get("email", String.class);
            return new UserPrincipal(UUID.fromString(sub), email != null ? email : sub);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    /** Returns the raw JWT string, or null if absent. Prefers Authorization header, falls back to cookie. */
    public String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
