package com.example.eam.WorkOrder.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.WorkOrder.Service.TmTimesheetSyncClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/timesheets/work-orders")
@RequiredArgsConstructor
@Validated
public class WorkOrderTimesheetSyncController {

    private final TmTimesheetSyncClient tmTimesheetSyncClient;
    private final ObjectMapper objectMapper;

    @PostMapping("/{workOrderId}/sync")
    public ResponseEntity<ApiResponse<Object>> syncWorkOrderToTm(
            @PathVariable Long workOrderId,
            @RequestParam(value = "companyId", required = false) Long companyId
    ) {
        if (workOrderId == null || workOrderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workOrderId must be greater than 0");
        }
        if (companyId == null || companyId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId must be greater than 0");
        }

        try {
            String tmResponse = tmTimesheetSyncClient.syncCompletedWorkOrder(workOrderId, companyId);
            return ResponseEntity.ok(
                    ApiResponse.successResponse(
                            HttpStatus.OK.value(),
                            "Timesheet sync completed",
                            parseResponseBody(tmResponse)
                    )
            );
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(ex.getStatusCode(), extractErrorReason(ex));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to sync timesheet with TM");
        }
    }

    private Object parseResponseBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node == null || node.isNull()) {
                return null;
            }
            return node;
        } catch (Exception ignored) {
            return body;
        }
    }

    private String extractErrorReason(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "TM timesheet sync failed";
        }
        return body;
    }
}
