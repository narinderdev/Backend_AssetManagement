package com.example.eam.Dashboard.Dto;

import com.example.eam.Enum.RequestPriority;
import com.example.eam.Enum.ServiceRequestStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NewServiceRequestDto {

    @JsonProperty("sr_db_id")
    private Long serviceRequestDbId;

    @JsonProperty("sr_id")
    private String serviceRequestId;

    private String title;
    private String asset;

    @JsonProperty("requester")
    private String requesterName;

    @JsonProperty("request_date")
    private LocalDateTime requestDate;

    private RequestPriority priority;
    private ServiceRequestStatus status;
}
