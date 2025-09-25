package com.duong.notification.repository.httpclient;

import com.duong.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Lấy tất cả theo user (sắp xếp createdAt desc)
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    // Lấy tất cả chưa đọc
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    // Paging
    Page<Notification> findByUserId(String userId, Pageable pageable);

    // Đếm số chưa đọc
    long countByUserIdAndReadFalse(String userId);
}
