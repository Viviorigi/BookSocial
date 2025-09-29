package com.duong.post.repository;

import com.duong.post.entity.Report;
import com.duong.post.entity.Report.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
}
