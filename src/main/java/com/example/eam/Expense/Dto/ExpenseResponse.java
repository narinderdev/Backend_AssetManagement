package com.example.eam.Expense.Dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExpenseResponse {
    private Long id;
    private Long companyId;
    private String expenseCode;
    private String expenseType;
    private String notes;
    private LocalDate date;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
