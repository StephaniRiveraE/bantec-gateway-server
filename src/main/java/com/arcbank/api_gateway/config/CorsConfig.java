package com.arcbank.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    // 1. Maneja el Preflight (OPTIONS) para evitar 403 Forbidden
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // O especificar dominios exactos
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    // 2. Filtro global que deduplica los headers CORS en la respuesta.
    // Los backends ya agregan CORS headers, y el CorsWebFilter también los agrega.
    // Este filtro se ejecuta después y elimina los valores duplicados.
    @Bean
    public GlobalFilter deduplicateCorsHeadersFilter() {
        return (exchange, chain) -> chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            deduplicateHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            deduplicateHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            deduplicateHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
            deduplicateHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
            deduplicateHeader(headers, HttpHeaders.ACCESS_CONTROL_MAX_AGE);
        }));
    }

    private void deduplicateHeader(HttpHeaders headers, String headerName) {
        List<String> values = headers.get(headerName);
        if (values != null && values.size() > 1) {
            // Mantener solo el primer valor único
            String first = values.get(0);
            headers.set(headerName, first);
        }
    }
}
