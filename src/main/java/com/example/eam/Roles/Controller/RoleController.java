package com.example.eam.Roles.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.PageResponse;
import com.example.eam.Roles.Dto.*;
import com.example.eam.Roles.Service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleCreateRequest req) {
        RoleResponse data = roleService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Role created successfully", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> patch(@PathVariable Long id, @RequestBody RolePatchRequest req) {
        RoleResponse data = roleService.patch(id, req);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Role updated successfully", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> get(@PathVariable Long id) {
        RoleResponse data = roleService.get(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Role fetched successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RoleResponse>>> list(Pageable pageable) {
        Page<RoleResponse> data = roleService.list(pageable);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Roles fetched successfully", PageResponse.from(data))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.successResponse(HttpStatus.OK.value(), "Role deleted successfully", null));
    }
}

