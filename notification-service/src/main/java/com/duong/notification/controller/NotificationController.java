package com.duong.notification.controller;

import com.duong.event.dto.NotificationEvent;
import com.duong.notification.dto.ApiResponse;
import com.duong.notification.dto.request.Recipient;
import com.duong.notification.dto.request.SendEmailRequest;
import com.duong.notification.dto.response.NotificationOut;
import com.duong.notification.entity.Notification;
import com.duong.notification.service.EmailService;
import com.duong.notification.service.NotificationService;
import com.duong.notification.service.NotificationStreamService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
public class NotificationController {

    EmailService emailService;
    NotificationService notificationService;
    NotificationStreamService streamService;

    private String me() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @KafkaListener(topics = "notification-delivery")
    public void listenNotificationDelivery(NotificationEvent message) {
        log.info("message received: {}", message);

        var p = message.getParam();

        // 1. Lưu in-app (vẫn đầy đủ trong DB nếu muốn)
        var saved = notificationService.createNotification(
                message.getRecipient(),
                message.getTemplateCode(),
                message.getSubject(),
                p
        );

        // 2. Gửi email nếu cần
        if ("EMAIL".equalsIgnoreCase(message.getChannel())) {
            emailService.sendEmail(SendEmailRequest.builder()
                    .to(Recipient.builder().email(message.getRecipient()).build())
                    .subject(message.getSubject())
                    .htmlContent(message.getBody())
                    .build());
        }

        // 3. Build payload gọn để SSE đẩy ra FE
        var out = NotificationOut.builder()
                .id(saved.getId())
                .type(message.getTemplateCode())
                .message((String) getOrNull(p, "message"))   // "Có bình luận mới" hoặc content ngắn
                .createdAt(saved.getCreatedAt())
                .actorId((String) getOrNull(p, "actorId"))
                .postId((String) getOrNull(p, "postId"))
                .commentId((String) getOrNull(p, "commentId"))
                .build();

        streamService.pushNotification(message.getRecipient(), out);
    }

    private Object getOrNull(Map<String, Object> map, String key) {
        return map == null ? null : map.get(key);
    }

    // tất cả notification
    @GetMapping
    public ApiResponse<List<Notification>> getAll() {
        return ApiResponse.<List<Notification>>builder()
                .result(notificationService.getAll(me()))
                .message("Lấy tất cả notification thành công")
                .build();
    }

    // notification chưa đọc
    @GetMapping("/unread")
    public ApiResponse<List<Notification>> getUnread() {
        return ApiResponse.<List<Notification>>builder()
                .result(notificationService.getUnread(me()))
                .message("Lấy notification chưa đọc thành công")
                .build();
    }

    // đếm noti chưa đọc
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.<Map<String, Long>>builder()
                .result(Map.of("unread", notificationService.unreadCount(me())))
                .message("Đếm số notification chưa đọc thành công")
                .build();
    }

    // page noti
    @GetMapping("/page")
    public ApiResponse<Page<Notification>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        var pr = notificationService.page(me(), page, size);

        return ApiResponse.<Page<Notification>>builder()
                .result(pr)
                .message("Lấy danh sách notification có phân trang thành công")
                .build();
    }

    // read 1
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id, me());
        return ApiResponse.<Void>builder()
                .message("Đánh dấu đã đọc thành công")
                .build();
    }

    // read all
    @PutMapping("/read-all")
    public ApiResponse<Map<String, Long>> markAllRead() {
        long modified = notificationService.markAllAsRead(me());
        return ApiResponse.<Map<String, Long>>builder()
                .result(Map.of("modified", modified))
                .message("Đánh dấu tất cả đã đọc thành công")
                .build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationOut>> stream() {
        String userId = me();
        var dataFlux = streamService.streamForUser(userId)
                .map(evt -> ServerSentEvent.<NotificationOut>builder()
                        .event(evt.getType())              // COMMENT / LIKE ...
                        .id(evt.getId())
                        .data(evt)
                        .build());

        var heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(t -> ServerSentEvent.<NotificationOut>builder()
                        .event("heartbeat")
                        .id("hb-" + t)
                        .data(null)                        // hoặc gửi 1 payload rỗng
                        .build());

        return Flux.merge(dataFlux, heartbeat);
    }

}
