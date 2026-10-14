package com.backend.rootly.filter;

import com.backend.rootly.config.RequestLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@Log4j2
public class RequestFilter extends OncePerRequestFilter {

    private static final String REDACTED = "[REDACTED]";
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie", "proxy-authorization", "x-api-key");
    private static final Pattern SENSITIVE_JSON_VALUE = Pattern.compile(
            "(?i)(\\\"(?:password|token|secret|authorization|otp|refreshToken|accessToken)\\\"\\s*:\\s*)"
                    + "\\\"(?:\\\\.|[^\\\"\\\\])*\\\"");
    private static final Pattern SENSITIVE_FORM_VALUE = Pattern.compile(
            "(?i)(password|token|secret|authorization|otp|refreshToken|accessToken)=([^&\\s]*)");
    private final RequestLoggingProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return properties.getExcludedPathPrefixes().stream()
                .anyMatch(prefix -> request.getRequestURI().startsWith(prefix));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        int cacheLimit = Math.max(0, properties.getMaxPayloadLength());
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, cacheLimit);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        long startedAt = System.nanoTime();

        if (log.isInfoEnabled()) {
            log.info("REQUEST BEGIN method={} uri={}", request.getMethod(), requestUri(request));
        }
        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            try {
                logDetails(cachedRequest, cachedResponse);
                if (log.isInfoEnabled()) {
                    long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
                    log.info("RESPONSE END status={} durationMs={}", cachedResponse.getStatus(), durationMillis);
                }
            } finally {
                cachedResponse.copyBodyToResponse();
            }
        }
    }

    private void logDetails(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        if (log.isDebugEnabled()) {
            log.debug("Request headers: {}", requestHeaders(request));
            logPayload("Request body", request.getContentAsByteArray(), request.getContentType(),
                    request.getCharacterEncoding());
            log.debug("Response headers: {}", responseHeaders(response));
            logPayload("Response body", response.getContentAsByteArray(), response.getContentType(),
                    response.getCharacterEncoding());
        }
    }

    private void logPayload(String label, byte[] content, String contentType, String encoding) {
        if (log.isDebugEnabled() && content.length > 0 && isTextContent(contentType)) {
            int maxLength = Math.max(0, properties.getMaxPayloadLength());
            int loggedLength = Math.min(content.length, maxLength);
            String payload = new String(content, 0, loggedLength, charset(encoding));
            String suffix = content.length > loggedLength ? "...[truncated]" : "";
            log.debug("{}: {}{}", label, redact(payload), suffix);
        }
    }

    private static boolean isTextContent(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith(MediaType.APPLICATION_JSON_VALUE)
                || normalized.startsWith(MediaType.TEXT_PLAIN_VALUE)
                || normalized.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                || normalized.contains("+json");
    }

    private static Charset charset(String encoding) {
        try {
            return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        } catch (IllegalArgumentException exception) {
            return StandardCharsets.UTF_8;
        }
    }

    private static Map<String, Object> requestHeaders(HttpServletRequest request) {
        Map<String, Object> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(
                name,
                SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT))
                        ? REDACTED
                        : Collections.list(request.getHeaders(name))));
        return headers;
    }

    private static Map<String, Object> responseHeaders(HttpServletResponse response) {
        Map<String, Object> headers = new LinkedHashMap<>();
        response.getHeaderNames().forEach(name -> headers.put(
                name,
                SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT))
                        ? REDACTED
                        : response.getHeaders(name)));
        return headers;
    }

    private static String requestUri(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + redact(query);
    }

    static String redact(String value) {
        String withoutJsonSecrets = SENSITIVE_JSON_VALUE.matcher(value).replaceAll("$1\\\"" + REDACTED + "\\\"");
        return SENSITIVE_FORM_VALUE.matcher(withoutJsonSecrets).replaceAll("$1=" + REDACTED);
    }
}