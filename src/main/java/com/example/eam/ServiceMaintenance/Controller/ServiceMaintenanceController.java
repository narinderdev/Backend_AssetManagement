package com.example.eam.ServiceMaintenance.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestCreateDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestApproveDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestRejectDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestResponse;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestUpdateDto;
import com.example.eam.ServiceMaintenance.Service.ServiceMaintenanceService;
import com.example.eam.WorkOrder.Dto.ConvertToWorkOrderRequest;
import com.example.eam.WorkOrder.Dto.WorkOrderDetailsResponse;
import com.example.eam.WorkOrder.Service.WorkOrderService;
import com.example.eam.WorkOrder.Dto.ConvertToWorkOrderRequest; 
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
public class ServiceMaintenanceController {

    private final ServiceMaintenanceService service;
    private final WorkOrderService workOrderService;



    // CREATE
    @PostMapping
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> create(
            @Valid @RequestBody ServiceRequestCreateDto dto) {

        ServiceRequestResponse created = service.create(dto);

        ApiResponse<ServiceRequestResponse> body =
                ApiResponse.successResponse(
                        HttpStatus.CREATED.value(),
                        "Service request created successfully",
                        created
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
    
    @PostMapping("/{id}/convert-to-wo")
    public ResponseEntity<ApiResponse<WorkOrderDetailsResponse>> convertToWorkOrder(
            @PathVariable Long id) { 

        WorkOrderDetailsResponse wo = workOrderService.convertServiceRequestToWorkOrder(id);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.successResponse(HttpStatus.CREATED.value(),
                        "Service request converted to work order successfully", wo)
        );
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ServiceRequestApproveDto dto) {

        ServiceRequestResponse resp = service.approve(id, dto);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Service request approved successfully", resp)
        );
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> reject(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequestRejectDto dto) {

        ServiceRequestResponse resp = service.reject(id, dto);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Service request rejected successfully", resp)
        );
    }


    // GET SINGLE
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> get(@PathVariable Long id) {

        ServiceRequestResponse resp = service.get(id);

        ApiResponse<ServiceRequestResponse> body =
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Service request fetched successfully",
                        resp
                );

        return ResponseEntity.ok(body);
    }

    // LIST (paginated)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ServiceRequestResponse>>> list(Pageable pageable) {

        Page<ServiceRequestResponse> page = service.list(pageable);

        ApiResponse<PageResponse<ServiceRequestResponse>> body =
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Service requests fetched successfully",
                        PageResponse.from(page)
                );

        return ResponseEntity.ok(body);
    }

    // PATCH UPDATE
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> update(
            @PathVariable Long id,
            @RequestBody ServiceRequestUpdateDto dto) {

        ServiceRequestResponse updated = service.update(id, dto);

        ApiResponse<ServiceRequestResponse> body =
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Service request updated successfully",
                        updated
                );

        return ResponseEntity.ok(body);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {

        service.delete(id);

        ApiResponse<Void> body =
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Service request deleted successfully",
                        null
                );

        return ResponseEntity.ok(body);
    }
}
