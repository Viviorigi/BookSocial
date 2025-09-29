package com.duong.post.controller;

import com.duong.post.dto.ApiResponse;
import com.duong.post.dto.response.ReportWithPostResponse;
import com.duong.post.entity.Report;
import com.duong.post.entity.Report.ReportStatus;
import com.duong.post.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {
    private final ReportService reportService;

    // Admin xem danh sách report theo status
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<ReportWithPostResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "PENDING") ReportStatus status) {

        var reports = reportService.getReportsWithPost(status, PageRequest.of(page, size));
        return ApiResponse.<Page<ReportWithPostResponse>>builder()
                .result(reports)
                .message("Fetched reports successfully")
                .build();
    }

    // Admin xử lý report
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Report> handle(@PathVariable String id,
                                      @RequestParam ReportStatus action) {

        var updated = reportService.handleReport(id, action);

        String msg = switch (action) {
            case APPROVED -> "Report approved successfully, post has been hidden";
            case REJECTED -> "Report rejected successfully";
            default -> "Report updated";
        };

        return ApiResponse.<Report>builder()
                .result(updated)
                .message(msg)
                .build();
    }
}
