package com.example.eam.WorkOrder.Service;

import com.example.eam.WorkOrder.Event.WorkOrderCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Component;

@Component
public class WorkOrderCompletionTmTimesheetSyncListener {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderCompletionTmTimesheetSyncListener.class);

    private final TmTimesheetSyncClient tmTimesheetSyncClient;

    public WorkOrderCompletionTmTimesheetSyncListener(TmTimesheetSyncClient tmTimesheetSyncClient) {
        this.tmTimesheetSyncClient = tmTimesheetSyncClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkOrderCompleted(WorkOrderCompletedEvent event) {
        Long workOrderId = event.workOrderId();
        Long companyId = event.companyId();

        if (workOrderId == null || companyId == null) {
            log.warn("TM timesheet sync skipped workOrderId={} companyId={} error=missing identifiers", workOrderId, companyId);
            return;
        }

        try {
            String response = tmTimesheetSyncClient.syncCompletedWorkOrder(workOrderId, companyId);
            log.info("TM timesheet sync succeeded workOrderId={} companyId={} response={}", workOrderId, companyId, response);
        } catch (RestClientResponseException ex) {
            log.warn(
                    "TM timesheet sync failed workOrderId={} companyId={} status={} error={}",
                    workOrderId,
                    companyId,
                    ex.getStatusCode().value(),
                    ex.getMessage()
            );
        } catch (Exception ex) {
            log.warn(
                    "TM timesheet sync failed workOrderId={} companyId={} status={} error={}",
                    workOrderId,
                    companyId,
                    "N/A",
                    ex.getMessage()
            );
        }
    }
}
