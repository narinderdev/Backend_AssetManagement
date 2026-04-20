package com.example.eam.WorkOrder.Service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TmTimesheetSyncClient {

    private final RestClient restClient;

    public TmTimesheetSyncClient(
            @Value("${tm.timesheet.base-url:http://localhost:8083}") String baseUrl,
            @Value("${tm.timesheet.connect-timeout:2s}") Duration connectTimeout,
            @Value("${tm.timesheet.read-timeout:5s}") Duration readTimeout) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public String syncCompletedWorkOrder(Long workOrderId, Long companyId) {
        return restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/timesheets/work-orders/{workOrderId}/sync")
                        .queryParam("companyId", companyId)
                        .build(workOrderId))
                .retrieve()
                .body(String.class);
    }
}
