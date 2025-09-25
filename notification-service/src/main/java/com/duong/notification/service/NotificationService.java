package com.duong.notification.service;

import com.duong.notification.entity.Notification;
import com.duong.notification.exception.AppException;
import com.duong.notification.exception.ErrorCode;
import com.duong.notification.repository.httpclient.NotificationRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;
    private final MongoTemplate mongoTemplate;

    public Notification createNotification(String userId, String type, String message, Map<String,Object> metadata) {
        Notification n = Notification.builder()
                .userId(userId).type(type).message(message)
                .metadata(metadata).read(false).createdAt(Instant.now())
                .build();
        return repo.save(n);
    }

    public List<Notification> getUnread(String userId) {
        return repo.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getAll(String userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }


    public void markAsRead(String notificationId, String userId) {
        Query q = new Query(Criteria.where("_id").is(notificationId)
                .and("userId").is(userId)
                .and("read").is(false));
        Update u = new Update().set("read", true);
        UpdateResult r = mongoTemplate.updateFirst(q, u, Notification.class);

        if (r.getMatchedCount() == 0) {
            // Không match: hoặc không tồn tại, hoặc không thuộc user, hoặc đã read rồi
            // kiểm tra để trả lỗi chính xác
            var n = repo.findById(notificationId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTI_NOT_FOUND));
            if (!n.getUserId().equals(userId)) throw new AppException(ErrorCode.UNAUTHORIZED);
            // else: đã read rồi -> coi như OK (idempotent)
        }
    }


    public long markAllAsRead(String userId) {
        Query q = new Query(Criteria.where("userId").is(userId).and("read").is(false));
        Update u = new Update().set("read", true);
        UpdateResult r = mongoTemplate.updateMulti(q, u, Notification.class);
        return r.getModifiedCount(); // số bản ghi được đánh dấu
    }


    public Page<Notification> page(String userId, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(size, 100));

        return repo.findByUserId(
                userId,
                PageRequest.of(p - 1, s, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    public long unreadCount(String userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }
}
