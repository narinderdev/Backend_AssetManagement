package com.example.eam.WorkOrder.Scheduler;

import com.example.eam.WorkOrder.Service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkOrderGeofenceScheduler {

    private final WorkOrderService workOrderService;

    @Value("${workorder.geofence.retention.days:90}")
    private int retentionDays;

    @Scheduled(cron = "${workorder.geofence.retention.cron:0 15 1 * * *}")
    public void purgeOldLocationLogs() {
        long deleted = workOrderService.purgeGeofenceLocationLogsOlderThanDays(retentionDays);
        if (deleted > 0) {
            log.info("Work-order geofence retention deleted {} location audit rows", deleted);
        }
    }
}
