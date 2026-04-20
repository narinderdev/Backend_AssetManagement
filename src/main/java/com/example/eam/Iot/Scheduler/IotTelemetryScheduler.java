package com.example.eam.Iot.Scheduler;

import com.example.eam.Iot.Service.IotAlertService;
import com.example.eam.Iot.Service.IotTelemetryIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IotTelemetryScheduler {

    private final IotTelemetryIngestService ingestService;
    private final IotAlertService alertService;

    @Value("${iot.retry.batch-size:500}")
    private int retryBatchSize;

    @Value("${iot.retention.days:180}")
    private int retentionDays;

    @Scheduled(fixedDelayString = "${iot.retry.interval-ms:300000}")
    public void retryUnprocessedTelemetry() {
        int processed = ingestService.retryPendingAndFailed(retryBatchSize);
        if (processed > 0) {
            log.info("IoT retry scheduler processed {} telemetry records", processed);
        }
    }

    @Scheduled(cron = "${iot.auto-resolve.cron:0 */10 * * * *}")
    public void autoResolveAlerts() {
        int resolved = alertService.autoResolveEligibleAlerts();
        if (resolved > 0) {
            log.info("IoT auto-resolve updated {} alerts", resolved);
        }
    }

    @Scheduled(cron = "${iot.retention.cron:0 30 1 * * *}")
    public void purgeTelemetryRetention() {
        long deleted = ingestService.purgeOlderThanDays(retentionDays);
        if (deleted > 0) {
            log.info("IoT retention deleted {} old telemetry rows", deleted);
        }
    }
}

