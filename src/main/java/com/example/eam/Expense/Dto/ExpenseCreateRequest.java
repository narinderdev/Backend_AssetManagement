package com.example.eam.Expense.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ExpenseCreateRequest {

    @NotBlank
    private String expenseCode;

    @NotBlank
    private String expenseType;

    @Size(max = 1000)
    private String notes;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}
