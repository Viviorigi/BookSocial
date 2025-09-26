package com.duong.gateway.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RateLimitConfig {
    private final ObjectMapper objectMapper;

    @Bean
    public KeyResolver clientKeyResolver() {
        return this::resolveKey;
    }

    private Mono<String> resolveKey(ServerWebExchange exchange) {
        var auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            var token = auth.substring(7);
            var userId = extractUserIdFromJwt(token);
            if (userId != null) return Mono.just("u:" + userId);
        }
        // Fallback IP
        return Mono.just("ip:" + extractClientIp(exchange));
    }

    private String extractClientIp(ServerWebExchange exchange) {
        var xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null) return xff.split(",")[0].trim();
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    private String extractUserIdFromJwt(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(json);
            if (node.hasNonNull("userId")) return node.get("userId").asText();
            if (node.hasNonNull("sub")) return node.get("sub").asText();
        } catch (Exception e) {
            log.debug("JWT parse error: {}", e.getMessage());
        }
        return null;
    }
}
