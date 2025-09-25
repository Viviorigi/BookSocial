package com.duong.post.messaging;

import com.duong.event.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${app.topics.notification-delivery}")
    private String topic;

    public void send(NotificationEvent event) {
        kafkaTemplate.send(topic, event.getRecipient(), event);
    }
}
