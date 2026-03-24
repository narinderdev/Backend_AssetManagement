package com.example.eam.InventoryManagement.Service;

import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Enum.ReorderStatus;
import com.example.eam.InventoryManagement.Dto.*;
import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Entity.InventoryReorderRequest;
import com.example.eam.InventoryManagement.Entity.Warehouse;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.InventoryManagement.Repository.InventoryReorderRequestRepository;
import com.example.eam.InventoryManagement.Repository.WarehouseRepository;
import com.example.eam.VendorManagement.Entity.Vendor;
import com.example.eam.VendorManagement.Repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class InventoryItemService {

    private final InventoryItemRepository itemRepo;
    private final WarehouseRepository warehouseRepo;
    private final VendorRepository vendorRepo;
    private final InventoryReorderRequestRepository reorderRepo;

    // -------- CREATE --------
    @Transactional
    public InventoryItemResponse create(InventoryItemCreateRequest dto) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);

        String itemId = determineItemId(dto.getItemId(), companyId);
        String skuNumber = normalizeSkuForCreate(dto.getSkuNumber(), companyId);

        Vendor vendor = null;
        if (dto.getPrimaryVendorDbId() != null) {
            vendor = vendorRepo.findByIdAndCompanyId(dto.getPrimaryVendorDbId(), companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor not found"));
        }

        Warehouse warehouse = null;
        if (dto.getWarehouseId() != null) {
            warehouse = warehouseRepo.findByIdAndDeletedFalseAndCompanyId(dto.getWarehouseId(), companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse not found"));
            if (!warehouse.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse is inactive");
            }
        }

        validateMinMax(dto.getMinStockLevel(), dto.getMaxStockLevel());

        InventoryItem item = InventoryItem.builder()
                .companyId(companyId)
                .itemId(itemId)
                .skuNumber(skuNumber)
                .itemName(dto.getItemName().trim())
                .category(normalizeCategory(dto.getCategory()))
                .unitOfMeasure(dto.getUnitOfMeasure())
                .manufacturer(dto.getManufacturer())
                .manufacturerPartNumber(dto.getManufacturerPartNumber())
                .stockLevel(dto.getStockLevel())
                .reorderPoint(dto.getReorderPoint())
                .reorderQuantity(dto.getReorderQuantity())
                .costPerUnit(dto.getCostPerUnit())
                .minStockLevel(dto.getMinStockLevel())
                .maxStockLevel(dto.getMaxStockLevel())
                .glAccountString(dto.getGlAccountString() != null ? dto.getGlAccountString().trim() : null)
                .expenseCode(dto.getExpenseCode() != null ? dto.getExpenseCode().trim() : null)
                .primaryVendor(vendor)
                .warehouse(warehouse)
                .active(dto.getActive() == null || dto.getActive())
                .deleted(false)
                .build();

        return toResponse(itemRepo.save(item));
    }

    // -------- PATCH UPDATE --------
    @Transactional
    public InventoryItemResponse patch(Long id, InventoryItemPatchRequest dto) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        InventoryItem item = getOrThrow(id, companyId);

        if (dto.getPrimaryVendorDbId() != null) {
            Vendor vendor = vendorRepo.findByIdAndCompanyId(dto.getPrimaryVendorDbId(), companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor not found"));
            item.setPrimaryVendor(vendor);
        }

        if (dto.getSkuNumber() != null) {
            String trimmed = dto.getSkuNumber().trim();
            if (trimmed.isEmpty()) {
                item.setSkuNumber(null);
            } else {
                ensureSkuUnique(trimmed, item.getId(), companyId);
                item.setSkuNumber(trimmed);
            }
        }

        if (dto.getItemName() != null) item.setItemName(dto.getItemName().trim());
        if (dto.getCategory() != null) item.setCategory(normalizeCategory(dto.getCategory()));
        if (dto.getUnitOfMeasure() != null) item.setUnitOfMeasure(dto.getUnitOfMeasure());
        if (dto.getManufacturer() != null) item.setManufacturer(dto.getManufacturer());
        if (dto.getManufacturerPartNumber() != null) item.setManufacturerPartNumber(dto.getManufacturerPartNumber());

        if (dto.getWarehouseId() != null) {
            if (dto.getWarehouseId() <= 0) {
                item.setWarehouse(null);
            } else {
                Warehouse warehouse = warehouseRepo.findById(dto.getWarehouseId())
                        .filter(w -> companyId == null || companyId.equals(w.getCompanyId()))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse not found"));
                if (!warehouse.isActive()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse is inactive");
                }
                item.setWarehouse(warehouse);
            }
        }

        if (dto.getStockLevel() != null) {
            if (dto.getStockLevel() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stockLevel cannot be negative");
            item.setStockLevel(dto.getStockLevel());
        }
        if (dto.getReorderPoint() != null) {
            if (dto.getReorderPoint() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reorderPoint cannot be negative");
            item.setReorderPoint(dto.getReorderPoint());
        }
        if (dto.getReorderQuantity() != null) {
            if (dto.getReorderQuantity() < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reorderQuantity must be >= 1");
            item.setReorderQuantity(dto.getReorderQuantity());
        }
        if (dto.getCostPerUnit() != null) {
            if (dto.getCostPerUnit().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "costPerUnit cannot be negative");
            }
            item.setCostPerUnit(dto.getCostPerUnit());
        }

        if (dto.getMinStockLevel() != null) item.setMinStockLevel(dto.getMinStockLevel());
        if (dto.getMaxStockLevel() != null) item.setMaxStockLevel(dto.getMaxStockLevel());
        validateMinMax(item.getMinStockLevel(), item.getMaxStockLevel());

        if (dto.getActive() != null) item.setActive(dto.getActive());

        if (dto.getGlAccountString() != null) {
            item.setGlAccountString(dto.getGlAccountString().trim());
        }
        if (dto.getExpenseCode() != null) {
            item.setExpenseCode(dto.getExpenseCode().trim());
        }

        return toResponse(itemRepo.save(item));
    }

    // -------- READ --------
    @Transactional(readOnly = true)
    public InventoryItemResponse get(Long id) {
        return toResponse(getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null)));
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> list(Pageable pageable) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        return itemRepo.findByDeletedFalseAndCompanyId(companyId, pageable).map(this::toResponse);
    }

    // -------- DELETE (soft delete) --------
    @Transactional
    public void delete(Long id) {
        InventoryItem item = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        item.setDeleted(true);
        item.setActive(false);
        itemRepo.save(item);
    }

    // -------- REORDER ACTION --------
    @Transactional
    public InventoryReorderResponse reorder(Long itemDbId, InventoryReorderCreateRequest dto) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        InventoryItem item = getOrThrow(itemDbId, companyId);

        Vendor vendor;
        if (dto.getVendorDbId() != null) {
            vendor = vendorRepo.findByIdAndCompanyId(dto.getVendorDbId(), companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor not found"));
        } else {
            vendor = item.getPrimaryVendor();
            if (vendor == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primary vendor not set for this item");
            }
        }

        if (vendor.getEmail() == null || vendor.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor email is missing. Cannot send reorder details.");
        }

        int qty = (dto.getQuantity() != null) ? dto.getQuantity() : item.getReorderQuantity();
        if (qty < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be >= 1");

        String reorderId = generateUniqueReorderId();

        BigDecimal unitCost = item.getCostPerUnit();
        BigDecimal total = (unitCost != null) ? unitCost.multiply(BigDecimal.valueOf(qty)) : null;

        InventoryReorderRequest rr = InventoryReorderRequest.builder()
                .reorderId(reorderId)
                .item(item)
                .vendor(vendor)
                .quantity(qty)
                .unitCostSnapshot(unitCost)
                .totalCostSnapshot(total)
                .status(ReorderStatus.CREATED) // keep CREATED; later you can integrate email/PO and mark SENT
                .requestedBy(dto.getRequestedBy())
                .deliveryLocation(dto.getDeliveryLocation())
                .note(dto.getNote())
                .build();

        InventoryReorderRequest saved = reorderRepo.save(rr);

        // “Send to vendor” integration point:
        // In production: publish event / send email / create PO in Procurement module.
        // For now: backend has created reorder request record with vendor + item details.

        return toReorderResponse(saved);
    }

    // ---------------- Helpers ----------------

    private InventoryItem getOrThrow(Long id, Long companyId) {
        return itemRepo.findByIdAndDeletedFalseAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
    }

    private String normalizeCategory(String category) {
        if (category == null) return null;
        String trimmed = category.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category cannot be blank");
        }
        return trimmed;
    }

    private void validateMinMax(Integer min, Integer max) {
        if (min != null && min < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minStockLevel cannot be negative");
        if (max != null && max < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxStockLevel cannot be negative");
        if (min != null && max != null && min > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minStockLevel cannot be greater than maxStockLevel");
        }
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        Vendor v = item.getPrimaryVendor();
        Warehouse wh = item.getWarehouse();
        return InventoryItemResponse.builder()
                .id(item.getId())
                .itemId(item.getItemId())
                .skuNumber(item.getSkuNumber())
                .itemName(item.getItemName())
                .category(item.getCategory())
                .unitOfMeasure(item.getUnitOfMeasure())
                .manufacturer(item.getManufacturer())
                .manufacturerPartNumber(item.getManufacturerPartNumber())
                .stockLevel(item.getStockLevel())
                .reorderPoint(item.getReorderPoint())
                .reorderQuantity(item.getReorderQuantity())
                .minStockLevel(item.getMinStockLevel())
                .maxStockLevel(item.getMaxStockLevel())
                .costPerUnit(item.getCostPerUnit())
                .glAccountString(item.getGlAccountString())
                .expenseCode(item.getExpenseCode())
                .primaryVendorDbId(v != null ? v.getId() : null)
                .primaryVendorName(v != null ? v.getVendorName() : null)
                .warehouseId(wh != null ? wh.getId() : null)
                .warehouseName(wh != null ? wh.getName() : null)
                .zoneAisle(wh != null ? wh.getZoneAisle() : null)
                .rackShelf(wh != null ? wh.getRackShelf() : null)
                .binCode(wh != null ? wh.getBinCode() : null)
                .binDescription(wh != null ? wh.getBinDescription() : null)
                .active(item.isActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private InventoryReorderResponse toReorderResponse(InventoryReorderRequest r) {
        return InventoryReorderResponse.builder()
                .id(r.getId())
                .reorderId(r.getReorderId())
                .itemDbId(r.getItem().getId())
                .itemId(r.getItem().getItemId())
                .itemName(r.getItem().getItemName())
                .vendorDbId(r.getVendor().getId())
                .vendorName(r.getVendor().getVendorName())
                .vendorEmail(r.getVendor().getEmail())
                .quantity(r.getQuantity())
                .status(r.getStatus())
                .requestedAt(r.getRequestedAt())
                .requestedBy(r.getRequestedBy())
                .deliveryLocation(r.getDeliveryLocation())
                .note(r.getNote())
                .build();
    }

    private String determineItemId(String providedItemId, Long companyId) {
        if (providedItemId != null && !providedItemId.trim().isEmpty()) {
            String trimmed = providedItemId.trim();
            if (itemRepo.existsByItemIdAndCompanyId(trimmed, companyId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Item ID already exists: " + trimmed);
            }
            return trimmed;
        }

        return generateUniqueItemId(companyId);
    }

    private String generateUniqueItemId(Long companyId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 30; attempt++) {
            int rand = ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("ITEM-%s-%04d", datePart, rand);
            if (!itemRepo.existsByItemIdAndCompanyId(candidate, companyId)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate unique Item ID");
    }

    private String generateUniqueReorderId() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 30; attempt++) {
            int rand = ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("RE-%s-%04d", datePart, rand);
            if (!reorderRepo.existsByReorderId(candidate)) return candidate;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate unique reorder ID");
    }

    private String normalizeSkuForCreate(String skuNumber, Long companyId) {
        if (skuNumber == null) return null;
        String trimmed = skuNumber.trim();
        if (trimmed.isEmpty()) return null;
        if (itemRepo.existsBySkuNumberAndDeletedFalseAndCompanyId(trimmed, companyId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists: " + trimmed);
        }
        return trimmed;
    }

    private void ensureSkuUnique(String skuNumber, Long currentItemId, Long companyId) {
        itemRepo.findBySkuNumberAndDeletedFalseAndCompanyId(skuNumber, companyId).ifPresent(existing -> {
            if (!existing.getId().equals(currentItemId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists: " + skuNumber);
            }
        });
    }
}

