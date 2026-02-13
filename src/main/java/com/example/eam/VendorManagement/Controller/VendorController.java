package com.example.eam.VendorManagement.Controller;


import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.VendorManagement.Dto.*;
import com.example.eam.VendorManagement.Service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Validated
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    public ResponseEntity<ApiResponse<VendorResponse>> create(@Valid @RequestBody VendorCreateRequest request) {
        VendorResponse data = vendorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Vendor created successfully", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponse>> patch(@PathVariable Long id,
                                                            @RequestBody VendorPatchRequest request) {
        VendorResponse data = vendorService.patch(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Vendor updated successfully", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponse>> get(@PathVariable Long id) {
        VendorResponse data = vendorService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Vendor fetched successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VendorResponse>>> list(
            Pageable pageable,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        Page<VendorResponse> data = vendorService.list(pageable, includeInactive);
        return ResponseEntity.ok(
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Vendors fetched successfully",
                        PageResponse.from(data)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vendorService.delete(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Vendor deleted successfully", null));
    }
}

