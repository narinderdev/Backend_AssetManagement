package com.example.eam.CompanyManagement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.CompanyManagement.Dto.CompanyCreateRequest;
import com.example.eam.CompanyManagement.Dto.CompanyPatchRequest;
import com.example.eam.CompanyManagement.Dto.CompanyResponse;
import com.example.eam.CompanyManagement.Service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Validated
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> create(@Valid @RequestBody CompanyCreateRequest request) {
        CompanyResponse data = companyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Company created successfully", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> patch(@PathVariable Long id,
                                                              @RequestBody CompanyPatchRequest request) {
        CompanyResponse data = companyService.patch(id, request);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Company updated successfully", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> get(@PathVariable Long id) {
        CompanyResponse data = companyService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Company fetched successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CompanyResponse>>> list(
            Pageable pageable,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        Page<CompanyResponse> data = companyService.list(pageable, includeInactive);
        return ResponseEntity.ok(
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Companies fetched successfully",
                        PageResponse.from(data)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Company deleted successfully", null));
    }
}
