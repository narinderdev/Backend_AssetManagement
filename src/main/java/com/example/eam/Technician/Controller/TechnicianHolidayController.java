package com.example.eam.Technician.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Technician.Dto.TechnicianHolidayListResponse;
import com.example.eam.Technician.Dto.TechnicianHolidayPatchRequest;
import com.example.eam.Technician.Dto.TechnicianHolidayRequest;
import com.example.eam.Technician.Dto.TechnicianHolidayResponse;
import com.example.eam.Technician.Service.TechnicianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public ResponseEntity<ApiResponse<TechnicianHolidayListResponse>> listHolidays(
            @RequestParam(value = "startDate", required = false) java.time.LocalDate startDate,
            @RequestParam(value = "endDate", required = false) java.time.LocalDate endDate) {
        TechnicianHolidayListResponse data = technicianService.listHolidays(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Holidays fetched successfully", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TechnicianHolidayResponse>> getHoliday(@PathVariable Long id) {
        TechnicianHolidayResponse data = technicianService.getHoliday(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Holiday fetched successfully", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TechnicianHolidayResponse>> patchHoliday(@PathVariable Long id,
                                                                               @Valid @RequestBody TechnicianHolidayPatchRequest request) {
        TechnicianHolidayResponse data = technicianService.patchHoliday(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Holiday updated successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        technicianService.deleteHoliday(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Holiday deleted successfully", null));
    }
}
