package com.example.eam.Technician.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Technician.Dto.TechnicianHolidayRequest;
import com.example.eam.Technician.Dto.TechnicianHolidayResponse;
import com.example.eam.Technician.Service.TechnicianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class TechnicianHolidayController {

    private final TechnicianService technicianService;

    @PostMapping
    public ResponseEntity<ApiResponse<TechnicianHolidayResponse>> addHoliday(
            @Valid @RequestBody TechnicianHolidayRequest request) {
        TechnicianHolidayResponse data = technicianService.addHoliday(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Holiday added successfully", data));
    }
}
