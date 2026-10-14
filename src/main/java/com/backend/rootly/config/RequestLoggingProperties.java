package com.backend.rootly.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "rootly.logging")
public class RequestLoggingProperties {

    private String applicationName = "Rootly";
    private int maxPayloadLength = 8192;
    private List<String> excludedPathPrefixes = List.of("/actuator");
}
