package com.example.eam.Iot.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Iot.Dto.IotTelemetryBatchRequest;
import com.example.eam.Iot.Dto.IotTelemetryBatchResponse;
import com.example.eam.Iot.Service.IotTelemetryIngestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/iot/v1")
@RequiredArgsConstructor
public class IotTelemetryIngestController {

    private final IotTelemetryIngestService telemetryIngestService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/telemetry")
    public ResponseEntity<ApiResponse<IotTelemetryBatchResponse>> ingest(
            @RequestHeader(name = "X-IoT-Device-Uid") String deviceUid,
            @RequestHeader(name = "X-IoT-Timestamp") String timestamp,
            @RequestHeader(name = "X-IoT-Nonce", required = false) String nonce,
            @RequestHeader(name = "X-IoT-Signature") String signature,
            @RequestBody String rawBody) {
        IotTelemetryBatchRequest request = parseAndValidate(rawBody);
        IotTelemetryBatchResponse response = telemetryIngestService.ingest(
                deviceUid,
                timestamp,
                nonce,
                signature,
                rawBody,
                request
        );
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Telemetry processed", response));
    }

    private IotTelemetryBatchRequest parseAndValidate(String rawBody) {
        try {
            IotTelemetryBatchRequest request = objectMapper.readValue(rawBody, IotTelemetryBatchRequest.class);
            Set<ConstraintViolation<IotTelemetryBatchRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String message = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining(", "));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
            return request;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid telemetry payload");
        }
    }
}

