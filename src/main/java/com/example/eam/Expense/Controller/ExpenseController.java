package com.example.eam.Expense.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Expense.Dto.ExpenseCreateRequest;
import com.example.eam.Expense.Dto.ExpenseResponse;
import com.example.eam.Expense.Service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @RequestParam("companyId") Long companyId,
            @Valid @RequestBody ExpenseCreateRequest request) {
        ExpenseResponse response = expenseService.create(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successResponse(HttpStatus.CREATED.value(), "Expense created", response));
    }
}
