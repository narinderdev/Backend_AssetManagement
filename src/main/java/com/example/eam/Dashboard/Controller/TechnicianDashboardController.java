package com.example.eam.Dashboard.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Dashboard.Dto.TechnicianDashboardResponse;
import com.example.eam.Dashboard.Service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/technicians")
@RequiredArgsConstructor
public class TechnicianDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<TechnicianDashboardResponse>> getDashboard(
            @org.springframework.web.bind.annotation.RequestParam(name = "limit", required = false) Integer limit
    ) {
        TechnicianDashboardResponse data = dashboardService.getTechnicianDashboard(limit);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(),
                "Technician dashboard fetched", data));
    }
}
