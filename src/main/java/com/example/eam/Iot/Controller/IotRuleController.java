package com.example.eam.Iot.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.Iot.Dto.IotRuleCreateRequest;
import com.example.eam.Iot.Dto.IotRulePatchRequest;
import com.example.eam.Iot.Dto.IotRuleResponse;
import com.example.eam.Iot.Service.IotAlertRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iot/rules")
@RequiredArgsConstructor
public class IotRuleController {

    private final IotAlertRuleService ruleService;

    @PostMapping
    public ResponseEntity<ApiResponse<IotRuleResponse>> create(@Valid @RequestBody IotRuleCreateRequest request) {
        IotRuleResponse response = ruleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "IoT alert rule created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IotRuleResponse>> get(@PathVariable Long id) {
        IotRuleResponse response = ruleService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT alert rule fetched", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<IotRuleResponse>>> list(Pageable pageable) {
        Page<IotRuleResponse> page = ruleService.list(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT alert rules fetched", PageResponse.from(page)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<IotRuleResponse>> patch(@PathVariable Long id,
                                                              @RequestBody IotRulePatchRequest request) {
        IotRuleResponse response = ruleService.patch(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT alert rule updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "IoT alert rule deleted", null));
    }
}

