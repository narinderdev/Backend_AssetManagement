package com.example.eam.WorkOrder.Dto;

public record WorkOrderInvoicePdfResult(
        String invoiceId,
        String fileName,
        byte[] pdfBytes
) {
}
