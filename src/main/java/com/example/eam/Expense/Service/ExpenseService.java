package com.example.eam.Expense.Service;

import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Expense.Dto.ExpenseCreateRequest;
import com.example.eam.Expense.Dto.ExpenseResponse;
import com.example.eam.Expense.Entity.Expense;
import com.example.eam.Expense.Repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional
    public ExpenseResponse create(Long companyId, ExpenseCreateRequest request) {
        Long scopedCompanyId = requireCompanyId();
        if (companyId == null || companyId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required");
        }
        if (!scopedCompanyId.equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId mismatch");
        }

        String expenseCode = normalizeRequired(request.getExpenseCode(), "expenseCode");
        String expenseType = normalizeRequired(request.getExpenseType(), "expenseType");

        Expense saved = expenseRepository.save(Expense.builder()
                .companyId(companyId)
                .expenseCode(expenseCode)
                .expenseType(expenseType)
                .notes(normalizeOptional(request.getNotes()))
                .expenseDate(request.getDate())
                .build());

        return toResponse(saved);
    }

    private Long requireCompanyId() {
        return CompanyContextHolder.getCompanyId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required"));
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ExpenseResponse toResponse(Expense entity) {
        return ExpenseResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .expenseCode(entity.getExpenseCode())
                .expenseType(entity.getExpenseType())
                .notes(entity.getNotes())
                .date(entity.getExpenseDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
