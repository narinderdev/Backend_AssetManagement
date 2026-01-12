package com.example.eam.Asset.Service;

import com.example.eam.Asset.Dto.*;
import com.example.eam.Asset.Entity.*;
import com.example.eam.Asset.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetLocationRepository locationRepository;
    private final AssetTechnicalDetailsRepository technicalRepository;
    private final AssetFinancialDetailsRepository financialRepository;
    private final AssetWarrantyLifecycleRepository warrantyRepository;
    private final AssetSafetyOperationsRepository safetyRepository;

    // ---------- CREATE (basic asset) ----------

    @Transactional
    public AssetDetailsResponse createAsset(CreateAssetDto request) {

        String assetId = determineAssetId(request.getAssetId());

        // Asset parent = null;
        // if (request.getParentAssetId() != null) {
        //     parent = assetRepository.findById(request.getParentAssetId())
        //             .orElseThrow(() -> new ResponseStatusException(
        //                     HttpStatus.BAD_REQUEST,
        //                     "Parent asset not found"
        //             ));
        // }

        Asset asset = Asset.builder()
                .assetId(assetId)
                .assetName(request.getAssetName())
                .shortDescription(request.getShortDescription())
                .assetCategory(request.getAssetCategory())
                .assetType(request.getAssetType())
                // .parentAsset(parent)
                .status(request.getStatus())
                .criticality(request.getCriticality())
                .ownership(request.getOwnership())
                .assetTag(request.getAssetTag())
                .build();

        Asset saved = assetRepository.save(asset);
        return getAssetDetails(saved.getId());
    }

    // ---------- CREATE / UPSERT SECTIONS (POST per table) ----------

    @Transactional
    public AssetDetailsResponse saveLocationOrg(Long assetId, AssetLocationDto dto) {
        Asset asset = getAssetOrThrow(assetId);

        AssetLocation loc = locationRepository.findByAsset_Id(assetId)
                .orElseGet(() -> {
                    AssetLocation l = new AssetLocation();
                    l.setAsset(asset);
                    return l;
                });

        loc.setLocation(dto.getLocation());
        loc.setDepartment(dto.getDepartment());
        loc.setCostCenter(dto.getCostCenter());
        loc.setAssignedOwner(dto.getAssignedOwner());
        loc.setMaintenanceTeam(dto.getMaintenanceTeam());

        locationRepository.save(loc);
        return getAssetDetails(assetId);
    }

    @Transactional
    public AssetDetailsResponse saveTechnicalDetails(Long assetId, AssetTechnicalDetailsDto dto) {
        Asset asset = getAssetOrThrow(assetId);

        AssetTechnicalDetails tech = technicalRepository.findByAsset_Id(assetId)
                .orElseGet(() -> {
                    AssetTechnicalDetails t = new AssetTechnicalDetails();
                    t.setAsset(asset);
                    return t;
                });

        tech.setManufacturer(dto.getManufacturer());
        tech.setModel(dto.getModel());
        tech.setSerialNumber(dto.getSerialNumber());
        tech.setYearOfManufacture(dto.getYearOfManufacture());
        tech.setPowerRating(dto.getPowerRating());
        tech.setVoltage(dto.getVoltage());
        tech.setCapacity(dto.getCapacity());

        technicalRepository.save(tech);
        return getAssetDetails(assetId);
    }

    @Transactional
    public AssetDetailsResponse saveFinancialDetails(Long assetId, AssetFinancialDetailsDto dto) {
        Asset asset = getAssetOrThrow(assetId);

        AssetFinancialDetails fin = financialRepository.findByAsset_Id(assetId)
                .orElseGet(() -> {
                    AssetFinancialDetails f = new AssetFinancialDetails();
                    f.setAsset(asset);
                    return f;
                });

        fin.setAcquisitionDate(dto.getAcquisitionDate());
        fin.setAcquisitionCost(dto.getAcquisitionCost());
        fin.setSupplier(dto.getSupplier());
        fin.setPoInvoiceNumber(dto.getPoInvoiceNumber());
        fin.setDepreciationMethod(dto.getDepreciationMethod());
        fin.setUsefulLifeYears(dto.getUsefulLifeYears());
        fin.setDepreciationStartDate(dto.getDepreciationStartDate());
        fin.setSalvageValue(dto.getSalvageValue());
        fin.setAccumulatedDepreciation(dto.getAccumulatedDepreciation());
        fin.setCurrentBookValue(dto.getCurrentBookValue());

        financialRepository.save(fin);
        return getAssetDetails(assetId);
    }

    @Transactional
    public AssetDetailsResponse saveWarrantyLifecycle(Long assetId, AssetWarrantyLifecycleDto dto) {
        Asset asset = getAssetOrThrow(assetId);

        AssetWarrantyLifecycle wl = warrantyRepository.findByAsset_Id(assetId)
                .orElseGet(() -> {
                    AssetWarrantyLifecycle w = new AssetWarrantyLifecycle();
                    w.setAsset(asset);
                    return w;
                });

        wl.setCommissioningDate(dto.getCommissioningDate());
        wl.setWarrantyStart(dto.getWarrantyStart());
        wl.setWarrantyEnd(dto.getWarrantyEnd());
        wl.setWarrantyProvider(dto.getWarrantyProvider());
        wl.setServiceContract(dto.getServiceContract());
        wl.setExpectedUsefulLifeYears(dto.getExpectedUsefulLifeYears());
        wl.setPlannedReplacementDate(dto.getPlannedReplacementDate());
        wl.setLastMaintenanceDate(dto.getLastMaintenanceDate());
        wl.setNextPlannedMaintenance(dto.getNextPlannedMaintenance());

        warrantyRepository.save(wl);
        return getAssetDetails(assetId);
    }

    @Transactional
    public AssetDetailsResponse saveSafetyOperations(Long assetId, AssetSafetyOperationsDto dto) {
        Asset asset = getAssetOrThrow(assetId);

        AssetSafetyOperations safety = safetyRepository.findByAsset_Id(assetId)
                .orElseGet(() -> {
                    AssetSafetyOperations s = new AssetSafetyOperations();
                    s.setAsset(asset);
                    return s;
                });

        safety.setSafetyCritical(dto.getSafetyCritical());
        safety.setSafetyNotes(dto.getSafetyNotes());
        safety.setOperatingInstructions(dto.getOperatingInstructions());

        safetyRepository.save(safety);
        return getAssetDetails(assetId);
    }

    // ---------- PATCH (single API to update anything) ----------

    @Transactional
    public AssetDetailsResponse patchAsset(Long id, AssetPatchRequest request) {
        Asset asset = getAssetOrThrow(id);

        // Basic
        if (request.getBasic() != null) {
            AssetBasicPatchRequest basic = request.getBasic();

            updateIfNotNull(basic.getAssetName(), asset::setAssetName);
            updateIfNotNull(basic.getShortDescription(), asset::setShortDescription);
            updateIfNotNull(basic.getAssetCategory(), asset::setAssetCategory);
            updateIfNotNull(basic.getAssetType(), asset::setAssetType);
            updateIfNotNull(basic.getOwnership(), asset::setOwnership);
            updateIfNotNull(basic.getAssetTag(), asset::setAssetTag);
            if (basic.getStatus() != null) {
                asset.setStatus(basic.getStatus());
            }
            if (basic.getCriticality() != null) {
                asset.setCriticality(basic.getCriticality());
            }
            // if (basic.getParentAssetId() != null) {
            //     Asset parent = assetRepository.findById(basic.getParentAssetId())
            //             .orElseThrow(() -> new ResponseStatusException(
            //                     HttpStatus.BAD_REQUEST,
            //                     "Parent asset not found"
            //             ));
            //     asset.setParentAsset(parent);
            // }
        }

        // Location section
        if (request.getLocationOrg() != null) {
            AssetLocationDto dto = request.getLocationOrg();
            AssetLocation loc = locationRepository.findByAsset_Id(id)
                    .orElseGet(() -> {
                        AssetLocation l = new AssetLocation();
                        l.setAsset(asset);
                        return l;
                    });

            updateIfNotNull(dto.getLocation(), loc::setLocation);
            updateIfNotNull(dto.getDepartment(), loc::setDepartment);
            updateIfNotNull(dto.getCostCenter(), loc::setCostCenter);
            updateIfNotNull(dto.getAssignedOwner(), loc::setAssignedOwner);
            updateIfNotNull(dto.getMaintenanceTeam(), loc::setMaintenanceTeam);

            locationRepository.save(loc);
        }

        // Technical section
        if (request.getTechnicalDetails() != null) {
            AssetTechnicalDetailsDto dto = request.getTechnicalDetails();
            AssetTechnicalDetails tech = technicalRepository.findByAsset_Id(id)
                    .orElseGet(() -> {
                        AssetTechnicalDetails t = new AssetTechnicalDetails();
                        t.setAsset(asset);
                        return t;
                    });

            updateIfNotNull(dto.getManufacturer(), tech::setManufacturer);
            updateIfNotNull(dto.getModel(), tech::setModel);
            updateIfNotNull(dto.getSerialNumber(), tech::setSerialNumber);
            updateIfNotNull(dto.getYearOfManufacture(), tech::setYearOfManufacture);
            updateIfNotNull(dto.getPowerRating(), tech::setPowerRating);
            updateIfNotNull(dto.getVoltage(), tech::setVoltage);
            updateIfNotNull(dto.getCapacity(), tech::setCapacity);

            technicalRepository.save(tech);
        }

        // Financial section
        if (request.getFinancialDetails() != null) {
            AssetFinancialDetailsDto dto = request.getFinancialDetails();
            AssetFinancialDetails fin = financialRepository.findByAsset_Id(id)
                    .orElseGet(() -> {
                        AssetFinancialDetails f = new AssetFinancialDetails();
                        f.setAsset(asset);
                        return f;
                    });

            updateIfNotNull(dto.getAcquisitionDate(), fin::setAcquisitionDate);
            updateIfNotNull(dto.getAcquisitionCost(), fin::setAcquisitionCost);
            updateIfNotNull(dto.getSupplier(), fin::setSupplier);
            updateIfNotNull(dto.getPoInvoiceNumber(), fin::setPoInvoiceNumber);
            updateIfNotNull(dto.getDepreciationMethod(), fin::setDepreciationMethod);
            updateIfNotNull(dto.getUsefulLifeYears(), fin::setUsefulLifeYears);
            updateIfNotNull(dto.getDepreciationStartDate(), fin::setDepreciationStartDate);
            updateIfNotNull(dto.getSalvageValue(), fin::setSalvageValue);
            updateIfNotNull(dto.getAccumulatedDepreciation(), fin::setAccumulatedDepreciation);
            updateIfNotNull(dto.getCurrentBookValue(), fin::setCurrentBookValue);

            financialRepository.save(fin);
        }

        // Warranty / lifecycle
        if (request.getWarrantyLifecycle() != null) {
            AssetWarrantyLifecycleDto dto = request.getWarrantyLifecycle();
            AssetWarrantyLifecycle wl = warrantyRepository.findByAsset_Id(id)
                    .orElseGet(() -> {
                        AssetWarrantyLifecycle w = new AssetWarrantyLifecycle();
                        w.setAsset(asset);
                        return w;
                    });

            updateIfNotNull(dto.getCommissioningDate(), wl::setCommissioningDate);
            updateIfNotNull(dto.getWarrantyStart(), wl::setWarrantyStart);
            updateIfNotNull(dto.getWarrantyEnd(), wl::setWarrantyEnd);
            updateIfNotNull(dto.getWarrantyProvider(), wl::setWarrantyProvider);
            updateIfNotNull(dto.getServiceContract(), wl::setServiceContract);
            updateIfNotNull(dto.getExpectedUsefulLifeYears(), wl::setExpectedUsefulLifeYears);
            updateIfNotNull(dto.getPlannedReplacementDate(), wl::setPlannedReplacementDate);
            updateIfNotNull(dto.getLastMaintenanceDate(), wl::setLastMaintenanceDate);
            updateIfNotNull(dto.getNextPlannedMaintenance(), wl::setNextPlannedMaintenance);

            warrantyRepository.save(wl);
        }

        // Safety / operations
        if (request.getSafetyOperations() != null) {
            AssetSafetyOperationsDto dto = request.getSafetyOperations();
            AssetSafetyOperations safety = safetyRepository.findByAsset_Id(id)
                    .orElseGet(() -> {
                        AssetSafetyOperations s = new AssetSafetyOperations();
                        s.setAsset(asset);
                        return s;
                    });

            updateIfNotNull(dto.getSafetyCritical(), safety::setSafetyCritical);
            updateIfNotNull(dto.getSafetyNotes(), safety::setSafetyNotes);
            updateIfNotNull(dto.getOperatingInstructions(), safety::setOperatingInstructions);

            safetyRepository.save(safety);
        }

        assetRepository.save(asset);
        return getAssetDetails(id);
    }

    // ---------- READ ----------

    @Transactional(readOnly = true)
    public AssetDetailsResponse getAssetDetails(Long id) {
        Asset asset = getAssetOrThrow(id);

        AssetLocation loc = locationRepository.findByAsset_Id(id).orElse(null);
        AssetTechnicalDetails tech = technicalRepository.findByAsset_Id(id).orElse(null);
        AssetFinancialDetails fin = financialRepository.findByAsset_Id(id).orElse(null);
        AssetWarrantyLifecycle wl = warrantyRepository.findByAsset_Id(id).orElse(null);
        AssetSafetyOperations safety = safetyRepository.findByAsset_Id(id).orElse(null);

        return mapToDetails(asset, loc, tech, fin, wl, safety);
    }

    @Transactional(readOnly = true)
    public Page<AssetDetailsResponse> listAssets(Pageable pageable) {
        return assetRepository.findAll(pageable)
                .map(asset -> {
                    Long id = asset.getId();
                    AssetLocation loc = locationRepository.findByAsset_Id(id).orElse(null);
                    AssetTechnicalDetails tech = technicalRepository.findByAsset_Id(id).orElse(null);
                    AssetFinancialDetails fin = financialRepository.findByAsset_Id(id).orElse(null);
                    AssetWarrantyLifecycle wl = warrantyRepository.findByAsset_Id(id).orElse(null);
                    AssetSafetyOperations safety = safetyRepository.findByAsset_Id(id).orElse(null);
                    return mapToDetails(asset, loc, tech, fin, wl, safety);
                });
    }

    // ---------- DELETE ----------

    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = getAssetOrThrow(id);
        try {
            assetRepository.delete(asset);
        } catch (DataIntegrityViolationException ex) {
            // Asset is referenced by other records (e.g., service requests/work orders)
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Asset cannot be deleted because it is referenced by other records"
            );
        }
    }

    // ---------- Helpers ----------

    private Asset getAssetOrThrow(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    }

    private <T> void updateIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private String determineAssetId(String providedAssetId) {
        if (providedAssetId != null && !providedAssetId.trim().isEmpty()) {
            String trimmedId = providedAssetId.trim();
            if (assetRepository.existsByAssetId(trimmedId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Asset ID '" + trimmedId + "' already exists");
            }
            return trimmedId;
        }

        return generateUniqueAssetId();
    }

    private String generateUniqueAssetId() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD

        for (int attempt = 0; attempt < 30; attempt++) {
            int rand = ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("AST-%s-%04d", datePart, rand);
            if (!assetRepository.existsByAssetId(candidate)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to generate unique Asset ID");
    }

    private AssetDetailsResponse mapToDetails(
            Asset asset,
            AssetLocation loc,
            AssetTechnicalDetails tech,
            AssetFinancialDetails fin,
            AssetWarrantyLifecycle wl,
            AssetSafetyOperations safety
    ) {
        AssetLocationDto locDto = null;
        if (loc != null) {
            locDto = new AssetLocationDto();
            locDto.setLocation(loc.getLocation());
            locDto.setDepartment(loc.getDepartment());
            locDto.setCostCenter(loc.getCostCenter());
            locDto.setAssignedOwner(loc.getAssignedOwner());
            locDto.setMaintenanceTeam(loc.getMaintenanceTeam());
        }

        AssetTechnicalDetailsDto techDto = null;
        if (tech != null) {
            techDto = new AssetTechnicalDetailsDto();
            techDto.setManufacturer(tech.getManufacturer());
            techDto.setModel(tech.getModel());
            techDto.setSerialNumber(tech.getSerialNumber());
            techDto.setYearOfManufacture(tech.getYearOfManufacture());
            techDto.setPowerRating(tech.getPowerRating());
            techDto.setVoltage(tech.getVoltage());
            techDto.setCapacity(tech.getCapacity());
        }

        AssetFinancialDetailsDto finDto = null;
        if (fin != null) {
            finDto = new AssetFinancialDetailsDto();
            finDto.setAcquisitionDate(fin.getAcquisitionDate());
            finDto.setAcquisitionCost(fin.getAcquisitionCost());
            finDto.setSupplier(fin.getSupplier());
            finDto.setPoInvoiceNumber(fin.getPoInvoiceNumber());
            finDto.setDepreciationMethod(fin.getDepreciationMethod());
            finDto.setUsefulLifeYears(fin.getUsefulLifeYears());
            finDto.setDepreciationStartDate(fin.getDepreciationStartDate());
            finDto.setSalvageValue(fin.getSalvageValue());
            finDto.setAccumulatedDepreciation(fin.getAccumulatedDepreciation());
            finDto.setCurrentBookValue(fin.getCurrentBookValue());
        }

        AssetWarrantyLifecycleDto wlDto = null;
        if (wl != null) {
            wlDto = new AssetWarrantyLifecycleDto();
            wlDto.setCommissioningDate(wl.getCommissioningDate());
            wlDto.setWarrantyStart(wl.getWarrantyStart());
            wlDto.setWarrantyEnd(wl.getWarrantyEnd());
            wlDto.setWarrantyProvider(wl.getWarrantyProvider());
            wlDto.setServiceContract(wl.getServiceContract());
            wlDto.setExpectedUsefulLifeYears(wl.getExpectedUsefulLifeYears());
            wlDto.setPlannedReplacementDate(wl.getPlannedReplacementDate());
            wlDto.setLastMaintenanceDate(wl.getLastMaintenanceDate());
            wlDto.setNextPlannedMaintenance(wl.getNextPlannedMaintenance());
        }

        AssetSafetyOperationsDto safetyDto = null;
        if (safety != null) {
            safetyDto = new AssetSafetyOperationsDto();
            safetyDto.setSafetyCritical(safety.getSafetyCritical());
            safetyDto.setSafetyNotes(safety.getSafetyNotes());
            safetyDto.setOperatingInstructions(safety.getOperatingInstructions());
        }

        return AssetDetailsResponse.builder()
                .id(asset.getId())
                .assetId(asset.getAssetId())
                .assetName(asset.getAssetName())
                .shortDescription(asset.getShortDescription())
                .assetCategory(asset.getAssetCategory())
                .assetType(asset.getAssetType())
                // .parentAssetId(asset.getParentAsset() != null ? asset.getParentAsset().getId() : null)
                .status(asset.getStatus())
                .criticality(asset.getCriticality())
                .ownership(asset.getOwnership())
                .assetTag(asset.getAssetTag())
                .location(locDto)
                .technicalDetails(techDto)
                .financialDetails(finDto)
                .warrantyLifecycle(wlDto)
                .safetyOperations(safetyDto)
                .build();
    }
}

