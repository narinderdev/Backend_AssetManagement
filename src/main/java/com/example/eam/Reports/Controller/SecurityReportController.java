package com.example.eam.Reports.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Reports.Dto.SecurityRoleReportItem;
import com.example.eam.Reports.Service.SecurityReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports/security")
@RequiredArgsConstructor
public class SecurityReportController {

    private final SecurityReportService securityReportService;

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<SecurityRoleReportItem>>> byRole() {
        List<SecurityRoleReportItem> data = securityReportService.getRolePermissionReport();
        return ResponseEntity.ok(
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Security report by role fetched successfully",
                        data
                )
        );
    }
}
