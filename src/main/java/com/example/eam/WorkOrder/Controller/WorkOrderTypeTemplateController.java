package com.example.eam.WorkOrder.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.WorkOrder.Dto.WorkOrderTypeTemplateCreateRequest;
import com.example.eam.WorkOrder.Dto.WorkOrderTypeTemplateResponse;
import com.example.eam.WorkOrder.Service.WorkOrderTypeTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-order-types")
@RequiredArgsConstructor
public class WorkOrderTypeTemplateController {

    private final WorkOrderTypeTemplateService service;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderTypeTemplateResponse>> create(
            @Valid @RequestBody WorkOrderTypeTemplateCreateRequest req) {
        WorkOrderTypeTemplateResponse data = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Work order type created", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrderTypeTemplateResponse>> get(@PathVariable Long id) {
        WorkOrderTypeTemplateResponse data = service.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order type fetched", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WorkOrderTypeTemplateResponse>>> list(Pageable pageable) {
        Page<WorkOrderTypeTemplateResponse> data = service.list(pageable);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Work order types fetched", data));
    }
}
