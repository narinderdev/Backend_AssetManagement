package com.example.eam.Procurement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Procurement.Dto.VendorReturnResponse;
import com.example.eam.Procurement.Service.VendorReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/procurement/returns")
@RequiredArgsConstructor
public class VendorReturnController {

    private final VendorReturnService vendorReturnService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VendorReturnResponse>>> list() {
        List<VendorReturnResponse> data = vendorReturnService.list(null, null, null);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Vendor returns fetched", data));
    }
}
