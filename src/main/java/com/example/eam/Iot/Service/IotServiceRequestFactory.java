package com.example.eam.Iot.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.IotAlertSeverity;
import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.Enum.ServiceRequestStatus;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import com.example.eam.ServiceMaintenance.Repository.ServiceMaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IotServiceRequestFactory {

    private final ServiceMaintenanceRepository serviceMaintenanceRepository;

    @Transactional
    public ServiceMaintenance createForAlert(Long companyId,
                                             Asset asset,
                                             String location,
                                             IotAlertSeverity severity,
                                             String title,
                                             String description,
                                             String deviceUid) {
        ServiceMaintenance request = ServiceMaintenance.builder()
                .companyId(companyId)
                .requestId(generateNextRequestId(companyId))
                .requestDate(LocalDateTime.now())
                .requesterName("IoT Monitor")
                .requesterContact(deviceUid)
                .asset(asset)
                .location(location)
                .maintenanceType(mapMaintenanceType(severity))
                .priority(mapPriority(severity))
                .shortTitle(title)
                .problemDescription(description)
                .status(ServiceRequestStatus.NEW)
                .deleted(false)
                .build();
        return serviceMaintenanceRepository.save(request);
    }

    private String generateNextRequestId(Long companyId) {
        String prefix = "SR-";
        long base = serviceMaintenanceRepository.findTopByCompanyIdOrderByIdDesc(companyId)
                .map(ServiceMaintenance::getId)
                .orElse(0L);

        String candidate;
        do {
            base++;
            candidate = prefix + String.format("%06d", base);
        } while (serviceMaintenanceRepository.existsByRequestIdAndCompanyId(candidate, companyId));

        return candidate;
    }

    private RequestPriority mapPriority(IotAlertSeverity severity) {
        if (severity == null) {
            return RequestPriority.MEDIUM;
        }
        return switch (severity) {
            case LOW -> RequestPriority.LOW;
            case MEDIUM -> RequestPriority.MEDIUM;
            case HIGH -> RequestPriority.HIGH;
            case CRITICAL -> RequestPriority.CRITICAL;
        };
    }

    private MaintenanceType mapMaintenanceType(IotAlertSeverity severity) {
        if (severity == IotAlertSeverity.CRITICAL) {
            return MaintenanceType.EMERGENCY;
        }
        return MaintenanceType.PREDICTIVE;
    }
}

