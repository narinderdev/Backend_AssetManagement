package com.example.eam.Iot.Service;

import com.example.eam.Config.IotMqttConfig;
import com.example.eam.Iot.Dto.IotMqttTelemetryMessage;
import com.example.eam.Iot.Dto.IotTelemetryBatchRequest;
import com.example.eam.Iot.Dto.IotTelemetryBatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "iot.mqtt", name = "enabled", havingValue = "true")
public class IotMqttTelemetryConsumer {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final IotTelemetryIngestService telemetryIngestService;

    @ServiceActivator(inputChannel = IotMqttConfig.IOT_MQTT_INPUT_CHANNEL)
    public void onMessage(Message<?> message) {
        String rawPayload = payloadAsString(message.getPayload());
        if (rawPayload == null || rawPayload.isBlank()) {
            log.warn("Ignoring empty MQTT telemetry payload");
            return;
        }

        try {
            IotMqttTelemetryMessage request = objectMapper.readValue(rawPayload, IotMqttTelemetryMessage.class);
            Set<ConstraintViolation<IotMqttTelemetryMessage>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String validationMessage = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining(", "));
                log.warn("Ignoring invalid MQTT telemetry payload: {}", validationMessage);
                return;
            }

            IotTelemetryBatchRequest batchRequest = new IotTelemetryBatchRequest();
            batchRequest.setReadings(request.getReadings());

            IotTelemetryBatchResponse response = telemetryIngestService.ingestFromMqtt(
                    request.getDeviceUid(),
                    request.getDeviceSecret(),
                    batchRequest
            );

            if (response.getFailedCount() > 0) {
                log.warn("Processed MQTT telemetry with {} failed rows for device {}", response.getFailedCount(), request.getDeviceUid());
            } else {
                log.debug("Processed MQTT telemetry: received={}, accepted={}, duplicate={} for device {}",
                        response.getReceivedCount(),
                        response.getAcceptedCount(),
                        response.getDuplicateCount(),
                        request.getDeviceUid());
            }
        } catch (Exception ex) {
            log.warn("Failed to process MQTT telemetry payload: {}", ex.getMessage());
        }
    }

    private String payloadAsString(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return payload.toString();
    }
}
