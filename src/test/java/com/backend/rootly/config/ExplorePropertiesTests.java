package com.backend.rootly.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ExplorePropertiesTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsConfiguredDurationsAndOrigins() {
        contextRunner.withPropertyValues("rootly.explore.cache-ttl=2m", "rootly.explore.max-stale=3h",
                "rootly.explore.allowed-origin-patterns=https://rootly.example,https://admin.rootly.example")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ExploreProperties properties = context.getBean(ExploreProperties.class);
                    assertThat(properties.getCacheTtl()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(properties.getMaxStale()).isEqualTo(Duration.ofHours(3));
                    assertThat(properties.getAllowedOriginPatterns())
                            .containsExactly("https://rootly.example", "https://admin.rootly.example");
                });
    }

    @Test
    void rejectsNonPositiveCacheTtlAtStartup() {
        contextRunner.withPropertyValues("rootly.explore.cache-ttl=0s")
                .run(context -> assertThat(context).hasFailed());
        contextRunner.withPropertyValues("rootly.explore.cache-ttl=-1s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsMaxStaleShorterThanCacheTtlAtStartup() {
        contextRunner.withPropertyValues("rootly.explore.cache-ttl=2m", "rootly.explore.max-stale=1m")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ExploreProperties.class)
    static class PropertiesConfiguration {
    }
}
