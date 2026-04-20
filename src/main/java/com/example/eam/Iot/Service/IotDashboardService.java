package com.example.eam.Iot.Service;

import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Iot.Dto.IotAlertResponse;
import com.example.eam.Iot.Dto.IotDashboardResponse;
import com.example.eam.Iot.Repository.IotAlertRepository;
import com.example.eam.Iot.Repository.IotDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IotDashboardService {

    private final IotDeviceRepository deviceRepository;
    private final IotAlertRepository alertRepository;
    private final IotAlertService alertService;

    @Transactional(readOnly = true)
    public IotDashboardResponse getDashboard() {
        Long companyId = requireCompanyId();
        long totalDevices = deviceRepository.countByCompanyIdAndEnabledTrue(companyId);
        long onlineDevices = deviceRepository.countOnlineEnabledDevices(companyId, LocalDateTime.now().minusMinutes(10));
        long offlineDevices = Math.max(totalDevices - onlineDevices, 0);

        long activeAlerts = alertService.countActiveAlerts(companyId);
        long criticalAlerts = alertService.countCriticalActiveAlerts(companyId);
        List<IotAlertResponse> recentIssues = alertRepository.findByCompanyIdOrderByOccurredAtDesc(
                        companyId,
                        PageRequest.of(0, 10))
                .stream()
                .map(alertService::toResponse)
                .toList();

        return IotDashboardResponse.builder()
                .totalDevices(totalDevices)
                .onlineDevices(onlineDevices)
                .offlineDevices(offlineDevices)
                .activeAlerts(activeAlerts)
                .criticalAlerts(criticalAlerts)
                .recentIssues(recentIssues)
                .build();
    }

    private Long requireCompanyId() {
        return CompanyContextHolder.getCompanyId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required"));
    }
}

