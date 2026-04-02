package com.example.eam.Iot.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Iot.Dto.IotDashboardResponse;
import com.example.eam.Iot.Service.IotDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iot/dashboard")
@RequiredArgsConstructor
public class IotDashboardController {

    private final IotDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<IotDashboardResponse>> getDashboard() {
        IotDashboardResponse response = dashboardService.getDashboard();
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT dashboard fetched", response));
    }
}

