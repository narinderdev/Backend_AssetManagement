package com.example.eam.Security.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Security.Dto.SecurityDashboardResponse;
import com.example.eam.Security.Service.SecurityDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/security-dashboard")
@RequiredArgsConstructor
public class SecurityDashboardController {

    private final SecurityDashboardService securityDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<SecurityDashboardResponse>> getDashboard(
            @RequestParam(value = "period", required = false, defaultValue = "THIS_WEEK") String period,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        SecurityDashboardResponse data = securityDashboardService.getDashboard(period, limit);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.successResponse(HttpStatus.OK.value(), "Security dashboard fetched", data));
    }
}
