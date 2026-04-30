package com.javaproject.splitewise.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestTracingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String REQUEST_ID = "requestId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = getOrGenerateId(request.getHeader(TRACE_ID_HEADER));
        String requestId = getOrGenerateId(request.getHeader(REQUEST_ID_HEADER));

        MDC.put(TRACE_ID, traceId);
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            log.info("request.started method={} path={} traceId={} requestId={}",
                    request.getMethod(), request.getRequestURI(), traceId, requestId);
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("request.completed method={} path={} status={} durationMs={} traceId={} requestId={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs, traceId, requestId);
            MDC.clear();
        }
    }

    private String getOrGenerateId(String incomingValue) {
        if (StringUtils.hasText(incomingValue)) {
            return incomingValue.trim();
        }
        return UUID.randomUUID().toString();
    }
}
