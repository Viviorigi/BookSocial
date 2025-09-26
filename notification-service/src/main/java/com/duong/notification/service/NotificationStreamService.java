package com.duong.notification.service;

import com.duong.notification.dto.response.NotificationOut;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationStreamService {
    // Mỗi userId có 1 sink (đa phát tới nhiều subscriber)
    private final Map<String, Sinks.Many<NotificationOut>> userSinks = new ConcurrentHashMap<>();

    public Flux<NotificationOut> streamForUser(String userId) {
        var sink = userSinks.computeIfAbsent(
                userId, k -> Sinks.many().multicast().onBackpressureBuffer()
        );

        // heartbeat mỗi 15s
        var heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(t -> NotificationOut.builder()
                        .id("hb-" + t)                 // id riêng để phân biệt
                        .type("HEARTBEAT")             // FE có thể bỏ qua
                        .message("ping")
                        .createdAt(Instant.now())
                        .build()
                );

        return Flux.merge(sink.asFlux(), heartbeat)
                .doOnCancel(() -> log.debug("SSE cancelled for user {}", userId));
    }

    public void pushNotification(String userId, NotificationOut payload) {
        var sink = userSinks.computeIfAbsent(userId,
                k -> Sinks.many().multicast().onBackpressureBuffer());

        var result = sink.tryEmitNext(payload);
        if (result.isFailure()) {
            log.warn("Emit failed for user {}: {}", userId, result);
        }
    }
}
