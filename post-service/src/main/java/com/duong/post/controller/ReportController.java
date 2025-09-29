package com.duong.post.controller;

import com.duong.post.dto.ApiResponse;
import com.duong.post.dto.request.ReportRequest;
import com.duong.post.entity.Report;
import com.duong.post.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    public ApiResponse<Report> report(@RequestBody ReportRequest req) {
        // Lấy userId từ JWT trong SecurityContext
        String me = SecurityContextHolder.getContext().getAuthentication().getName();

        var saved = reportService.createReport(me, req.getPostId(), req.getReason());
        return ApiResponse.<Report>builder()
                .result(saved)
                .message("Report created successfully")
                .build();
    }
}
