package com.example.eam.WorkOrder.Service;

import com.example.eam.Common.PdfGeneratorService;
import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.WorkOrder.Dto.InvoiceTechnicianRateRequest;
import com.example.eam.WorkOrder.Dto.WorkOrderInvoicePdfResult;
import com.example.eam.WorkOrder.Dto.WorkOrderInvoiceRequest;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Entity.WorkOrderLaborEntry;
import com.example.eam.WorkOrder.Entity.WorkOrderMaterialUsage;
import com.example.eam.WorkOrder.Repository.WorkOrderLaborEntryRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderMaterialUsageRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import com.example.eam.Technician.Entity.Technician;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderInvoiceService {

    private static final String BRAND_NAME = "Finlware";
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-uuuu");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-uuuu HH:mm");
    private static final String TEMPLATE_PATH = "templates/invoice.html";

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderLaborEntryRepository laborEntryRepository;
    private final WorkOrderMaterialUsageRepository materialUsageRepository;
    private final PdfGeneratorService pdfGeneratorService;

    @Transactional(readOnly = true)
    public WorkOrderInvoicePdfResult generateInvoice(Long workOrderId, WorkOrderInvoiceRequest request) {
        try {
            WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order not found"));

            if (request.getDueDate().isBefore(request.getInvoiceDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueDate cannot be before invoiceDate");
            }

            String currency = HtmlUtils.htmlEscape(resolveCurrency(request.getCurrencySymbol()));
            Map<Long, BigDecimal> rateMap = toRateMap(request.getTechnicianRates());

            List<WorkOrderLaborEntry> laborEntries = laborEntryRepository.findByWorkOrder_Id(workOrderId);
            List<WorkOrderMaterialUsage> materialUsages = materialUsageRepository.findByWorkOrder_Id(workOrderId);

            LaborTotals laborTotals = buildLaborRows(laborEntries, rateMap, currency);
            MaterialTotals materialTotals = buildMaterialRows(materialUsages, currency);

            BigDecimal laborSubtotal = laborTotals.subtotal;
            BigDecimal partsSubtotal = materialTotals.subtotal;
            BigDecimal total = laborSubtotal.add(partsSubtotal).setScale(2, RoundingMode.HALF_UP);

            String invoiceId = buildInvoiceId(workOrderId);
            String fileName = "invoice-" + workOrder.getWorkOrderId() + "-" + invoiceId + ".pdf";

            String html = renderHtml(request, workOrder, laborTotals, materialTotals, laborSubtotal, partsSubtotal, total, invoiceId, currency);
            byte[] pdfBytes = pdfGeneratorService.generatePdf(html);

            return new WorkOrderInvoicePdfResult(invoiceId, fileName, pdfBytes);
        } catch (ResponseStatusException ex) {
            throw ex; // propagate validation / not-found as-is
        } catch (Exception ex) {
            log.error("Unexpected error during invoice generation for workOrderId={}", workOrderId, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate invoice");
        }
    }

    private String resolveCurrency(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "$";
        }
        return symbol.trim();
    }

    private Map<Long, BigDecimal> toRateMap(List<InvoiceTechnicianRateRequest> rates) {
        if (CollectionUtils.isEmpty(rates)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "technicianRates are required");
        }
        Map<Long, BigDecimal> map = new HashMap<>();
        for (InvoiceTechnicianRateRequest rate : rates) {
            if (rate.getTechnicianId() == null || rate.getHourlyRate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "technicianId and hourlyRate are required");
            }
            map.put(rate.getTechnicianId(), rate.getHourlyRate().setScale(2, RoundingMode.HALF_UP));
        }
        return map;
    }

    private LaborTotals buildLaborRows(List<WorkOrderLaborEntry> entries, Map<Long, BigDecimal> rateMap, String currency) {
        StringBuilder rows = new StringBuilder();
        BigDecimal subtotal = BigDecimal.ZERO;

        if (entries != null) {
            List<WorkOrderLaborEntry> sorted = entries.stream()
                    .sorted(Comparator.comparing(e -> e.getTechnician() != null ? e.getTechnician().getId() : Long.MAX_VALUE))
                    .toList();

            for (WorkOrderLaborEntry entry : sorted) {
                Technician tech = entry.getTechnician();
                Long techId = tech != null ? tech.getId() : null;
                String techName = resolveTechnicianName(entry);
                BigDecimal hours = defaultZero(entry.getLaborHours());
                BigDecimal rate = resolveRate(rateMap, techId, entry.getHourlyRate(), techName);
                BigDecimal amount = hours.multiply(rate).setScale(2, RoundingMode.HALF_UP);

                subtotal = subtotal.add(amount);
                rows.append("<tr>")
                        .append("<td>").append(escape(techName)).append("</td>")
                        .append("<td>").append(escape(defaultText(entry.getNotes(), "Work completed"))).append("</td>")
                        .append("<td class='num'>").append(formatHours(hours)).append("</td>")
                        .append("<td class='num'>").append(currency).append(" ").append(rate.toPlainString()).append("</td>")
                        .append("<td class='num'>").append(currency).append(" ").append(amount.toPlainString()).append("</td>")
                        .append("</tr>");
            }
        }

        if (rows.isEmpty()) {
            rows.append("<tr><td colspan='5' class='muted'>No labor entries recorded</td></tr>");
        }

        return new LaborTotals(rows.toString(), subtotal.setScale(2, RoundingMode.HALF_UP));
    }

    private MaterialTotals buildMaterialRows(List<WorkOrderMaterialUsage> usages, String currency) {
        StringBuilder rows = new StringBuilder();
        BigDecimal subtotal = BigDecimal.ZERO;

        if (usages != null) {
            List<WorkOrderMaterialUsage> sorted = usages.stream()
                    .sorted(Comparator.comparing(WorkOrderMaterialUsage::getId))
                    .toList();

            for (WorkOrderMaterialUsage usage : sorted) {
                InventoryItem item = usage.getInventoryItem();
                String code = item != null ? defaultText(item.getItemId(), "-") : "-";
                String name = item != null ? defaultText(item.getItemName(), "-") : "-";
                String uom = item != null && item.getUnitOfMeasure() != null ? item.getUnitOfMeasure().name() : "-";

                BigDecimal qty = new BigDecimal(Objects.toString(usage.getQuantityUsed(), "0"));
                BigDecimal unitPrice = defaultZero(usage.getUnitCostSnapshot());
                if (unitPrice.compareTo(BigDecimal.ZERO) == 0 && item != null && item.getCostPerUnit() != null) {
                    unitPrice = item.getCostPerUnit();
                }

                BigDecimal lineTotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(lineTotal);

                rows.append("<tr>")
                        .append("<td>").append(escape(code)).append("</td>")
                        .append("<td>").append(escape(name)).append("</td>")
                        .append("<td>").append(escape(uom)).append("</td>")
                        .append("<td class='num'>").append(qty).append("</td>")
                        .append("<td class='num'>").append(currency).append(" ").append(unitPrice.toPlainString()).append("</td>")
                        .append("<td class='num'>0%</td>")
                        .append("<td class='num'>").append(currency).append(" ").append(lineTotal.toPlainString()).append("</td>")
                        .append("</tr>");
            }
        }

        if (rows.isEmpty()) {
            rows.append("<tr><td colspan='7' class='muted'>No inventory usage recorded</td></tr>");
        }

        return new MaterialTotals(rows.toString(), subtotal.setScale(2, RoundingMode.HALF_UP));
    }

    private String renderHtml(
            WorkOrderInvoiceRequest request,
            WorkOrder workOrder,
            LaborTotals labor,
            MaterialTotals materials,
            BigDecimal laborSubtotal,
            BigDecimal partsSubtotal,
            BigDecimal total,
            String invoiceId,
            String currency
    ) {
        String template = loadTemplate();

        String billTo = escape(request.getCompanyName());
        String billAddress = escape(request.getCompanyAddress()).replace("\n", "<br/>");
        String contactLine = buildContactLine(request.getContactName(), request.getContactNumber());

        String workOrderDescription = defaultText(workOrder.getDescriptionScope(), "N/A");
        String workOrderTitle = defaultText(workOrder.getWoTitle(), workOrderDescription);
        String assigned = resolveAssignedTo(workOrder);
        String completedOn = formatDateTime(resolveCompletedOn(workOrder));
        String assetLine = workOrder.getAsset() != null
                ? workOrder.getAsset().getAssetName() + " (" + workOrder.getAsset().getAssetId() + ")"
                : "N/A";
        String location = defaultText(workOrder.getLocation(), "N/A");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("brandName", BRAND_NAME);
        placeholders.put("brandLine1", "Invoice generated for completed work order.");
        placeholders.put("brandLine2", "");
        placeholders.put("invoiceNumber", invoiceId);
        placeholders.put("invoiceDate", formatDate(request.getInvoiceDate()));
        placeholders.put("dueDate", formatDate(request.getDueDate()));
        placeholders.put("status", "Unpaid");
        placeholders.put("workOrderId", escape(workOrder.getWorkOrderId()));
        placeholders.put("workOrderTitle", escape(workOrderTitle));
        placeholders.put("billToName", billTo);
        placeholders.put("billToAddress", billAddress);
        placeholders.put("billToContact", contactLine);
        placeholders.put("assetLine", escape(assetLine));
        placeholders.put("location", escape(location));
        placeholders.put("issue", escape(workOrderDescription));
        placeholders.put("completedBy", escape(assigned));
        placeholders.put("completedOn", escape(completedOn));
        placeholders.put("laborRows", labor.rowsHtml);
        placeholders.put("partsRows", materials.rowsHtml);
        placeholders.put("inventoryNote", "Inventory used is auto-picked from material issues linked to the work order.");
        placeholders.put("laborSubtotal", currency + " " + formatMoney(laborSubtotal));
        placeholders.put("partsSubtotal", currency + " " + formatMoney(partsSubtotal));
        placeholders.put("discount", currency + " 0.00");
        placeholders.put("tax", currency + " 0.00");
        placeholders.put("total", currency + " " + formatMoney(total));
        placeholders.put("amountPaid", currency + " 0.00");
        placeholders.put("balanceDue", currency + " " + formatMoney(total));
        placeholders.put("terms", "Payment due on or before " + formatDate(request.getDueDate())
                + ".<br/>Warranty: 30 days on workmanship; parts warranty as per manufacturer.<br/>Payment: Bank Transfer / UPI / Cheque.");

        String html = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return html;
    }

    private String loadTemplate() {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try {
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Invoice template not found at {}", TEMPLATE_PATH, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invoice template missing");
        }
    }

    private String buildInvoiceId(Long workOrderId) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
        return "INV-" + date + "-" + workOrderId + "-" + random;
    }

    private String resolveTechnicianName(WorkOrderLaborEntry entry) {
        Technician tech = entry.getTechnician();
        if (tech != null && tech.getFullName() != null && !tech.getFullName().isBlank()) {
            return tech.getFullName();
        }
        if (entry.getTechnicianNameSnapshot() != null && !entry.getTechnicianNameSnapshot().isBlank()) {
            return entry.getTechnicianNameSnapshot();
        }
        return "Technician";
    }

    /**
     * Resolve rate for an entry. Allows team-only labor rows (no technician id) by using either the
     * entry's own hourlyRate snapshot or, if absent, a single default rate supplied in the request.
     */
    private BigDecimal resolveRate(Map<Long, BigDecimal> rateMap, Long techId, BigDecimal fallbackRate, String techName) {
        if (techId != null && rateMap.containsKey(techId)) {
            return rateMap.get(techId);
        }
        if (fallbackRate != null) {
            return fallbackRate.setScale(2, RoundingMode.HALF_UP);
        }
        if (techId == null && !rateMap.isEmpty()) {
            // Work order could be assigned to a team; fall back to the first provided rate.
            return rateMap.values().iterator().next();
        }
        String identifier = techId != null ? "id " + techId : (techName != null ? "name " + techName : "unassigned");
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing rate for technician " + identifier);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatHours(BigDecimal hours) {
        return hours == null ? "0.00" : hours.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatMoney(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DAY_FORMAT.format(date);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMAT.format(dateTime);
    }

    private LocalDateTime resolveCompletedOn(WorkOrder wo) {
        if (wo.getActualEndDateTime() != null) return wo.getActualEndDateTime();
        if (wo.getActualStartDateTime() != null) return wo.getActualStartDateTime();
        if (wo.getPlannedEndDateTime() != null) return wo.getPlannedEndDateTime();
        return wo.getPlannedStartDateTime();
    }

    private String resolveAssignedTo(WorkOrder wo) {
        if (wo.getAssignedTeam() != null && wo.getAssignedTeam().getTeamName() != null) {
            return "Team - " + wo.getAssignedTeam().getTeamName();
        }
        if (wo.getAssignedTechnician() != null && wo.getAssignedTechnician().getFullName() != null) {
            return "Technician - " + wo.getAssignedTechnician().getFullName();
        }
        return "Unassigned";
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value;
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private String buildContactLine(String name, String phone) {
        StringJoiner joiner = new StringJoiner(" | ");
        if (name != null && !name.isBlank()) joiner.add(escape(name));
        if (phone != null && !phone.isBlank()) joiner.add(HtmlUtils.htmlEscape(phone));
        String combined = joiner.toString();
        return combined.isBlank() ? "Contact: -" : "Contact: " + combined;
    }

    private record LaborTotals(String rowsHtml, BigDecimal subtotal) {
    }

    private record MaterialTotals(String rowsHtml, BigDecimal subtotal) {
    }
}
