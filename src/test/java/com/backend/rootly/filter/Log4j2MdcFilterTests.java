package com.backend.rootly.filter;

import com.backend.rootly.config.RequestLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class Log4j2MdcFilterTests {

    @Test
    void addsCorrelationValuesAndClearsThemAfterTheRequest() throws Exception {
        RequestLoggingProperties properties = new RequestLoggingProperties();
        properties.setApplicationName("Rootly");
        Log4j2MdcFilter filter = new Log4j2MdcFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-B3-TraceId", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceId = new AtomicReference<>();
        AtomicReference<String> requestId = new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) -> {
            traceId.set(MDC.get("traceId"));
            requestId.set(MDC.get("REQ_ID"));
        };
        filter.doFilter(request, response, chain);

        assertThat(traceId.get()).isEqualTo("trace-123");
        assertThat(requestId.get()).isNotBlank();
        assertThat(response.getHeader(Log4j2MdcFilter.REQUEST_ID_HEADER)).isEqualTo(requestId.get());
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("REQ_ID")).isNull();
        assertThat(MDC.get("NAME")).isNull();
    }

    @Test
    void replacesUnsafeIncomingCorrelationValues() throws IOException, ServletException {
        RequestLoggingProperties properties = new RequestLoggingProperties();
        Log4j2MdcFilter filter = new Log4j2MdcFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Log4j2MdcFilter.REQUEST_ID_HEADER, "unsafe value with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(response.getHeader(Log4j2MdcFilter.REQUEST_ID_HEADER))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
