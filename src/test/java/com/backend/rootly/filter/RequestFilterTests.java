package com.backend.rootly.filter;

import com.backend.rootly.config.RequestLoggingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RequestFilterTests {

    @Test
    void preservesRequestAndResponseBodies() throws Exception {
        RequestLoggingProperties properties = new RequestLoggingProperties();
        RequestFilter filter = new RequestFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"email\":\"test@example.com\",\"password\":\"secret\"}"
                .getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            String body = new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(body).contains("test@example.com");
            servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            servletResponse.getWriter().write("{\"status\":\"ok\"}");
        });

        assertThat(response.getContentAsString()).isEqualTo("{\"status\":\"ok\"}");
    }

    @Test
    void redactsCredentialsAndTokensFromLoggedPayloads() {
        String json = "{\"password\":\"secret\",\"token\":\"jwt\",\"name\":\"Rootly\"}";
        String form = "email=user@example.com&password=secret&refreshToken=token-value";

        assertThat(RequestFilter.redact(json))
                .isEqualTo("{\"password\":\"[REDACTED]\",\"token\":\"[REDACTED]\",\"name\":\"Rootly\"}");
        assertThat(RequestFilter.redact(form))
                .isEqualTo("email=user@example.com&password=[REDACTED]&refreshToken=[REDACTED]");
    }
}
