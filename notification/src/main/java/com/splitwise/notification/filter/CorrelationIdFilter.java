package com.splitwise.notification.filter;

import com.splitwise.notification.logging.LogContextKeys;
import com.splitwise.notification.logging.MdcScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = readOrGenerate(request.getHeader(CORRELATION_HEADER));
        String requestId = UUID.randomUUID().toString();
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        Object authenticatedUserId = request.getAttribute("authenticatedUserId");
        String userId = authenticatedUserId == null ? "" : authenticatedUserId.toString();

        try (MdcScope ignored = MdcScope.with(Map.of(
                LogContextKeys.CORRELATION_ID, correlationId,
                LogContextKeys.REQUEST_ID, requestId,
                LogContextKeys.USER_ID, userId == null ? "" : userId
        ))) {
            LOGGER.info("Incoming request method={} path={}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        }
    }

    private String readOrGenerate(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return headerValue;
    }
}
