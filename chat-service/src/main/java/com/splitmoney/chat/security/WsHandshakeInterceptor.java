package com.splitmoney.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    static final String WS_USER_ID_ATTR = "wsUserId";
    static final String WS_USER_EMAIL_ATTR = "wsUserEmail";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final JwtTokenService jwtTokenService;

    public WsHandshakeInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return false;
        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        String token = resolveToken(httpRequest);
        if (token == null) return false;

        try {
            Claims claims = jwtTokenService.parseToken(token);
            if ("refresh".equals(claims.get("type", String.class))) return false;
            attributes.put(WS_USER_ID_ATTR, UUID.fromString(claims.getSubject()));
            String email = claims.get("email", String.class);
            if (email != null) attributes.put(WS_USER_EMAIL_ATTR, email);
            attributes.put("wsToken", token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception
    ) {}

    private String resolveToken(HttpServletRequest request) {
        Optional<String> fromCookie = Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(c -> ACCESS_TOKEN_COOKIE.equals(c.getName()))
                        .map(Cookie::getValue)
                        .filter(v -> !v.isBlank())
                        .findFirst());
        if (fromCookie.isPresent()) return fromCookie.get();

        // Fallback: ?token= query param (for clients that can't set cookies on WS)
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) return queryToken;

        return null;
    }
}
