package com.example.eam.Procurement.Service;

import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.Procurement.Dto.CreateGrnRequest;
import com.example.eam.Procurement.Dto.GrnLineResponse;
import com.example.eam.Procurement.Dto.GrnResponse;
import com.example.eam.Procurement.Entity.GoodsReceiptNote;
import com.example.eam.Procurement.Entity.GoodsReceiptNoteLine;
import com.example.eam.Procurement.Entity.PurchaseOrder;
import com.example.eam.Procurement.Entity.PurchaseOrderLine;
import com.example.eam.Procurement.Entity.StockLedgerEntry;
import com.example.eam.Procurement.Enum.PurchaseOrderStatus;
import com.example.eam.Procurement.Enum.StockMovementType;
import com.example.eam.Procurement.Enum.StockReferenceType;
import com.example.eam.Procurement.Repository.GoodsReceiptNoteRepository;
import com.example.eam.Procurement.Repository.PurchaseOrderRepository;
import com.example.eam.Procurement.Repository.StockLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GRNService {

    private final GoodsReceiptNoteRepository grnRepository;
    private final PurchaseOrderRepository poRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockLedgerEntryRepository stockLedgerEntryRepository;
    private final NumberGeneratorService numberGeneratorService;

    @Transactional
    public GrnResponse create(CreateGrnRequest request) {
        PurchaseOrder po = null;
        Map<Long, PurchaseOrderLine> poLinesById = new HashMap<>();
        if (request.getPoId() != null) {
            po = poRepository.findById(request.getPoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase Order not found"));
            if (po.getStatus() != PurchaseOrderStatus.DELIVERED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "GRN can only be created when PO status is DELIVERED");
            }
            List<PurchaseOrderLine> poLines = Optional.ofNullable(po.getLines()).orElseGet(Collections::emptyList);
            if (poLines.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Purchase Order has no lines");
            }
            poLinesById = poLines.stream()
                    .collect(Collectors.toMap(PurchaseOrderLine::getId, Function.identity()));
        }

        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one GRN line is required");
        }

        Instant now = Instant.now();
        GoodsReceiptNote grn = GoodsReceiptNote.builder()
                .grnNumber(numberGeneratorService.generateGrnNumber())
                .poId(po != null ? po.getId() : null)
                .vendorId(po != null ? po.getVendorId() : null)
                .receivedByUserId(requireText(request.getReceivedByUserId(), "receivedByUserId is required"))
                .receivedAtUtc(now)
                .dayKeyUtc(DateTimeFormatter.ISO_LOCAL_DATE.format(now.atZone(ZoneOffset.UTC).toLocalDate()))
                .notes(trim(request.getNotes()))
                .build();

        Map<Long, BigDecimal> qtyByItem = new HashMap<>();
        for (var lineRequest : request.getLines()) {
            PurchaseOrderLine poLine = null;
            if (po != null) {
                poLine = poLinesById.get(lineRequest.getPoLineId());
                if (poLine == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PO line does not belong to this PO: " + lineRequest.getPoLineId());
                }
            }
            if (po == null && lineRequest.getPoLineId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "poId is required when specifying poLineId");
            }
            BigDecimal orderedQty = null;
            if (poLine != null) {
                orderedQty = poLine.getOrderedQty();
            } else {
                orderedQty = lineRequest.getOrderedQty();
                if (orderedQty == null || orderedQty.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderedQty must be greater than zero when no PO is provided");
                }
                orderedQty = orderedQty.setScale(4, RoundingMode.HALF_UP);
            }

            BigDecimal receivedQty = lineRequest.getReceivedQty();
            if (receivedQty == null || receivedQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "receivedQty must be greater than zero");
            }
            receivedQty = receivedQty.setScale(4, RoundingMode.HALF_UP);

            BigDecimal returnQty = lineRequest.getReturnQty();
            if (returnQty != null) {
                if (returnQty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "returnQty cannot be negative");
                }
                returnQty = returnQty.setScale(4, RoundingMode.HALF_UP);
            }

            if (poLine != null) {
                BigDecimal newTotal = poLine.getReceivedQty().add(receivedQty);
                if (newTotal.compareTo(poLine.getOrderedQty()) > 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Over receipt detected for PO line " + poLine.getId());
                }
                poLine.setReceivedQty(newTotal);
                grn.addLine(GoodsReceiptNoteLine.builder()
                        .grn(grn)
                        .poLineId(poLine.getId())
                        .itemId(poLine.getItemId())
                        .orderedQty(orderedQty)
                        .receivedQty(receivedQty)
                        .returnQty(returnQty)
                        .build());
                qtyByItem.merge(poLine.getItemId(), receivedQty, BigDecimal::add);
            } else {
                // Non-PO GRN requires itemId from request
                if (lineRequest.getPoLineId() != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "poLineId not allowed when poId is absent");
                }
                if (lineRequest.getItemId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemId is required for GRN lines without PO");
                }
                grn.addLine(GoodsReceiptNoteLine.builder()
                        .grn(grn)
                        .poLineId(null)
                        .itemId(lineRequest.getItemId())
                        .orderedQty(orderedQty)
                        .receivedQty(receivedQty)
                        .returnQty(returnQty)
                        .build());
                qtyByItem.merge(lineRequest.getItemId(), receivedQty, BigDecimal::add);
            }
        }

        GoodsReceiptNote saved = grnRepository.save(grn);
        incrementStock(qtyByItem, saved.getId());

        if (po != null) {
            if (po.getLines().stream().allMatch(line -> line.getOrderedQty().compareTo(line.getReceivedQty()) == 0)) {
                po.setStatus(PurchaseOrderStatus.CLOSED);
            }
            po.setUpdatedAt(Instant.now());
            poRepository.save(po);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public GrnResponse get(Long id) {
        GoodsReceiptNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GRN not found"));
        return toResponse(grn);
    }

    @Transactional(readOnly = true)
    public List<GrnResponse> list(Long poId) {
        List<GoodsReceiptNote> grns = (poId != null) ? grnRepository.findByPoId(poId) : grnRepository.findAll();
        return grns.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GrnResponse> listByDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from date cannot be after to date");
        }
        String fromKey = DateTimeFormatter.ISO_LOCAL_DATE.format(from);
        String toKey = DateTimeFormatter.ISO_LOCAL_DATE.format(to);
        return grnRepository.findByDayKeyUtcBetween(fromKey, toKey).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GrnResponse> listByMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be between 1 and 12");
        }
        String prefix = String.format("%04d-%02d", year, month);
        return grnRepository.findByDayKeyUtcStartingWith(prefix).stream()
                .map(this::toResponse)
                .toList();
    }

    // Helpers

    private void incrementStock(Map<Long, BigDecimal> qtyByItem, Long grnId) {
        if (qtyByItem.isEmpty()) return;

        List<StockLedgerEntry> ledgerEntries = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : qtyByItem.entrySet()) {
            Long itemId = entry.getKey();
            BigDecimal qty = entry.getValue();
            InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory item not found: " + itemId));

            int delta = toWholeUnits(qty);
            item.setStockLevel(item.getStockLevel() + delta);
            inventoryItemRepository.save(item);

            ledgerEntries.add(StockLedgerEntry.builder()
                    .itemId(itemId)
                    .refType(StockReferenceType.GRN)
                    .refId(grnId)
                    .movementType(StockMovementType.IN)
                    .qty(qty.setScale(4, RoundingMode.HALF_UP))
                    .build());
        }

        if (!ledgerEntries.isEmpty()) {
            stockLedgerEntryRepository.saveAll(ledgerEntries);
        }
    }

    private int toWholeUnits(BigDecimal qty) {
        BigDecimal normalized = qty.stripTrailingZeros();
        if (normalized.scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory stock supports whole units; receivedQty must be a whole number");
        }
        try {
            return normalized.intValueExact();
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "receivedQty is too large for stock counter");
        }
    }

    private GrnResponse toResponse(GoodsReceiptNote grn) {
        List<GrnLineResponse> lines = Optional.ofNullable(grn.getLines())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(line -> GrnLineResponse.builder()
                        .id(line.getId())
                        .poLineId(line.getPoLineId())
                        .itemId(line.getItemId())
                        .orderedQty(line.getOrderedQty())
                        .receivedQty(line.getReceivedQty())
                        .returnQty(line.getReturnQty())
                        .build())
                .toList();

        return GrnResponse.builder()
                .id(grn.getId())
                .grnNumber(grn.getGrnNumber())
                .poId(grn.getPoId())
                .vendorId(grn.getVendorId())
                .receivedByUserId(grn.getReceivedByUserId())
                .receivedAtUtc(grn.getReceivedAtUtc())
                .dayKeyUtc(grn.getDayKeyUtc())
                .notes(grn.getNotes())
                .createdAt(grn.getCreatedAt())
                .updatedAt(grn.getUpdatedAt())
                .lines(lines)
                .build();
    }

    private String trim(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private String requireText(String value, String message) {
        String t = trim(value);
        if (t == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return t;
    }
}
