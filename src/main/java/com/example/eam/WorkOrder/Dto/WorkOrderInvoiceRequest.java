package com.example.eam.WorkOrder.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderInvoiceRequest {

    /**
     * Customer (bill-to) name.
     */
    @NotBlank
    private String companyName;

    /**
     * Customer address (single string; line breaks may be included).
     */
    @NotBlank
    private String companyAddress;

    private String contactName;
    private String contactNumber;

    @NotNull
    private LocalDate invoiceDate;

    @NotNull
    private LocalDate dueDate;

    /**
     * Currency symbol to prepend to monetary values. Defaults to "$" when null/blank.
     */
    private String currencySymbol;

    /**
     * One rate entry per technician involved in the work order labor entries.
     */
    @Valid
    @NotEmpty
    private List<InvoiceTechnicianRateRequest> technicianRates;
}
