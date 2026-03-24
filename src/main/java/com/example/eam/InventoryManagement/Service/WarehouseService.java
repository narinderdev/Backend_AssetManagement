package com.example.eam.InventoryManagement.Service;

import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.InventoryManagement.Dto.WarehouseCreateRequest;
import com.example.eam.InventoryManagement.Dto.WarehousePatchRequest;
import com.example.eam.InventoryManagement.Dto.WarehouseResponse;
import com.example.eam.InventoryManagement.Entity.Warehouse;
import com.example.eam.InventoryManagement.Repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepo;

    @Transactional
    public WarehouseResponse create(WarehouseCreateRequest dto) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        String name = normalizeName(dto.getName());
        if (warehouseRepo.existsByNameIgnoreCaseAndDeletedFalseAndCompanyId(name, companyId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse name already exists");
        }

        Warehouse warehouse = Warehouse.builder()
                .companyId(companyId)
                .name(name)
                .address(trimToNull(dto.getAddress()))
                .zoneAisle(trimToNull(dto.getZoneAisle()))
                .rackShelf(trimToNull(dto.getRackShelf()))
                .binCode(trimToNull(dto.getBinCode()))
                .binDescription(trimToNull(dto.getBinDescription()))
                .active(dto.getActive() == null || dto.getActive())
                .deleted(false)
                .build();

        return toResponse(warehouseRepo.save(warehouse));
    }

    @Transactional
    public WarehouseResponse patch(Long id, WarehousePatchRequest dto) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        Warehouse warehouse = getOrThrow(id, companyId);

        if (dto.getName() != null) {
            String name = normalizeName(dto.getName());
            warehouseRepo.findByNameIgnoreCaseAndDeletedFalseAndCompanyId(name, companyId).ifPresent(existing -> {
                if (!existing.getId().equals(warehouse.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse name already exists");
                }
            });
            warehouse.setName(name);
        }

        if (dto.getAddress() != null) {
            warehouse.setAddress(trimToNull(dto.getAddress()));
        }

        if (dto.getZoneAisle() != null) {
            warehouse.setZoneAisle(trimToNull(dto.getZoneAisle()));
        }
        if (dto.getRackShelf() != null) {
            warehouse.setRackShelf(trimToNull(dto.getRackShelf()));
        }
        if (dto.getBinCode() != null) {
            warehouse.setBinCode(trimToNull(dto.getBinCode()));
        }
        if (dto.getBinDescription() != null) {
            warehouse.setBinDescription(trimToNull(dto.getBinDescription()));
        }

        if (dto.getActive() != null) {
            warehouse.setActive(dto.getActive());
        }

        return toResponse(warehouseRepo.save(warehouse));
    }

    @Transactional(readOnly = true)
    public WarehouseResponse get(Long id) {
        return toResponse(getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null)));
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> listActive() {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        return warehouseRepo.findByActiveTrueAndDeletedFalseAndCompanyId(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Warehouse warehouse = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        warehouse.setActive(false);
        warehouse.setDeleted(true);
        warehouseRepo.save(warehouse);
    }

    // -------- helpers ----------
    private Warehouse getOrThrow(Long id, Long companyId) {
        Warehouse warehouse = companyId != null
                ? warehouseRepo.findByIdAndDeletedFalseAndCompanyId(id, companyId).orElse(null)
                : warehouseRepo.findById(id).orElse(null);
        if (warehouse == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found");
        }
        if (warehouse.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found");
        }
        return warehouse;
    }

    private Warehouse getOrThrow(Long id) {
        Warehouse warehouse = warehouseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
        if (warehouse.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found");
        }
        return warehouse;
    }

    private String normalizeName(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse name is required");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse name cannot be blank");
        }
        return trimmed;
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .address(warehouse.getAddress())
                .zoneAisle(warehouse.getZoneAisle())
                .rackShelf(warehouse.getRackShelf())
                .binCode(warehouse.getBinCode())
                .binDescription(warehouse.getBinDescription())
                .active(warehouse.isActive())
                .createdAt(warehouse.getCreatedAt())
                .updatedAt(warehouse.getUpdatedAt())
                .build();
    }

    private String trimToNull(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
