package com.backend.rootly.filter;

import com.backend.rootly.config.RequestLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class Log4j2MdcFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-B3-TraceId";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");
    private final RequestLoggingProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = correlationId(request.getHeader(REQUEST_ID_HEADER));
        String traceId = correlationId(request.getHeader(TRACE_ID_HEADER), requestId);

        MDC.put("traceId", traceId);
        MDC.put("REQ_ID", requestId);
        MDC.put("NAME", properties.getApplicationName().toUpperCase(Locale.ROOT));
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            MDC.remove("REQ_ID");
            MDC.remove("NAME");
        }
    }

    private static String correlationId(String candidate) {
        return correlationId(candidate, UUID.randomUUID().toString());
    }

    private static String correlationId(String candidate, String fallback) {
        return candidate != null && SAFE_CORRELATION_ID.matcher(candidate).matches() ? candidate : fallback;
    }
}
