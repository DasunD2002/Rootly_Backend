package com.backend.rootly.config;

import com.backend.rootly.utility.EndPoint;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExploreProperties.class)
@RequiredArgsConstructor
public class ExploreConfiguration {

    private final ExploreProperties properties;

    @Bean
    public HttpClient wikidataHttpClient() {
        return HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
    }

    @Bean
    public URI wikidataQueryUri(
            @Value("classpath:queries/explore-places.sparql") Resource query) throws IOException {
        String encoded = URLEncoder.encode(query.getContentAsString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        return URI.create(properties.getWikidataUrl() + "?format=json&query=" + encoded);
    }

    @Bean
    public Clock exploreClock() {
        return Clock.systemUTC();
    }

    @Bean
    @org.springframework.core.annotation.Order(1)
    // Spring's HttpSecurity builder declares checked Exception in its API.
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public SecurityFilterChain exploreSecurity(HttpSecurity http) throws Exception {
        http.securityMatcher(EndPoint.API + EndPoint.EXPLORE_PATH_PATTERN)
                .cors(Customizer.withDefaults())
                // The public places POST performs a read-only search without session credentials.
                .csrf(csrf -> csrf.ignoringRequestMatchers(EndPoint.API + EndPoint.EXPLORE_PLACES,
                        EndPoint.API + EndPoint.EXPLORE_PROVINCE))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, EndPoint.API + EndPoint.EXPLORE_PLACES,
                                EndPoint.API + EndPoint.EXPLORE_PROVINCE).permitAll()
                        .requestMatchers(HttpMethod.GET, EndPoint.API + EndPoint.EXPLORE_CATEGORIES,
                                EndPoint.API + EndPoint.EXPLORE_PLACE_DETAIL).permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean(name = "corsConfigurationSource")
    public CorsConfigurationSource exploreCors() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(properties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Accept", "Content-Type"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(EndPoint.API + EndPoint.EXPLORE_PATH_PATTERN, configuration);
        return source;
    }
}
