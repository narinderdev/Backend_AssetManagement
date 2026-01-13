package com.example.eam.Maintenance.Preventive.Scheduler;

import com.example.eam.Maintenance.Preventive.Service.PreventivePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PreventivePlanScheduler {

    private final PreventivePlanService planService;

    // TEMP: run daily at 00:00 server time for testing PM WO generation
    @Scheduled(cron = "0 0 0 * * *")
    public void generateDue() {
        planService.generateDueWorkOrders();
    }
}
