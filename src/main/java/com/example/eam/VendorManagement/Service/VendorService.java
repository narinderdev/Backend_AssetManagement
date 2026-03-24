package com.example.eam.VendorManagement.Service;

import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Enum.VendorStatus;
import com.example.eam.VendorManagement.Dto.*;
import com.example.eam.VendorManagement.Entity.Vendor;
import com.example.eam.VendorManagement.Repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    @Transactional
    public VendorResponse create(VendorCreateRequest req) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        // Determine vendor ID: use provided one or generate new one
        String vendorId = determineVendorId(req.getVendorId(), companyId);

        Vendor vendor = Vendor.builder()
                .companyId(companyId)
                .vendorId(vendorId)
                .vendorName(req.getVendorName().trim())
                .taxId(normalizeAndEnsureUniqueTaxId(req.getTaxId(), null, companyId))
                .address(req.getAddress())
                .contactPerson(req.getContactPerson().trim())
                .email(req.getEmail().trim())
                .phone(req.getPhone().trim())
                .paymentTerms(req.getPaymentTerms())
                .rating(req.getRating())
                .status(VendorStatus.PENDING)
                .active(req.getActive() == null || req.getActive())
                .build();

        Vendor saved = vendorRepository.save(vendor);
        return toResponse(saved);
    }

    @Transactional
    public VendorResponse patch(Long id, VendorPatchRequest req) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        Vendor vendor = getOrThrowActive(id, companyId);

        updateIfNotBlank(req.getVendorName(), vendor::setVendorName);
        updateIfNotBlank(req.getAddress(), vendor::setAddress);
        updateIfNotBlank(req.getContactPerson(), vendor::setContactPerson);
        updateIfNotBlank(req.getEmail(), vendor::setEmail);
        updateIfNotBlank(req.getPhone(), vendor::setPhone);
        if (req.getTaxId() != null) {
            String taxId = req.getTaxId().trim();
            if (taxId.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taxId cannot be blank");
            }
            ensureTaxIdUnique(taxId, vendor.getId(), companyId);
            vendor.setTaxId(taxId);
        }

        if (req.getPaymentTerms() != null) vendor.setPaymentTerms(req.getPaymentTerms());
        if (req.getRating() != null) vendor.setRating(req.getRating());
        if (req.getActive() != null) vendor.setActive(req.getActive());

        Vendor saved = vendorRepository.save(vendor);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VendorResponse get(Long id) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        return toResponse(getOrThrowActiveAndVisible(id, companyId));
    }

    @Transactional(readOnly = true)
    public Page<VendorResponse> list(Pageable pageable, boolean includeInactive) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        List<VendorStatus> visible = List.of(VendorStatus.APPROVED, VendorStatus.PENDING);
        Page<Vendor> page = includeInactive
                ? vendorRepository.findByStatusInAndCompanyId(visible, companyId, pageable)
                : vendorRepository.findByActiveTrueAndStatusInAndCompanyId(visible, companyId, pageable);

        return page.map(this::toResponse);
    }

    @Transactional
    public VendorResponse approve(Long id) {
        Vendor vendor = getOrThrowActive(id, CompanyContextHolder.getCompanyId().orElse(null));
        if (vendor.getStatus() != VendorStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor is already " + vendor.getStatus().name().toLowerCase());
        }
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setRejectionComment(null);
        return toResponse(vendorRepository.save(vendor));
    }

    @Transactional
    public VendorResponse reject(Long id, VendorRejectRequest req) {
        Vendor vendor = getOrThrowActive(id, CompanyContextHolder.getCompanyId().orElse(null));
        if (vendor.getStatus() != VendorStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor is already " + vendor.getStatus().name().toLowerCase());
        }
        vendor.setStatus(VendorStatus.REJECTED);
        vendor.setRejectionComment(req.getComment().trim());
        return toResponse(vendorRepository.save(vendor));
    }

    /**
     * Soft delete: set active=false (recommended for ERP/EAM audits).
     */
    @Transactional
    public void delete(Long id) {
        Vendor vendor = getOrThrowActive(id, CompanyContextHolder.getCompanyId().orElse(null));
        vendor.setActive(false);
        vendorRepository.save(vendor);
    }

    // ---------------- Helpers ----------------

    private Vendor getOrThrowActive(Long id, Long companyId) {
        return vendorRepository.findByIdAndActiveTrueAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
    }

    private Vendor getOrThrowActiveAndVisible(Long id, Long companyId) {
        Vendor vendor = getOrThrowActive(id, companyId);
        if (vendor.getStatus() == VendorStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found or rejected");
        }
        return vendor;
    }

    private void updateIfNotBlank(String value, Consumer<String> setter) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value.trim());
        }
    }

    /**
     * Determines vendor ID: uses provided ID if valid, otherwise generates a new one.
     * Validates that the provided ID is unique.
     */
    private String determineVendorId(String providedVendorId, Long companyId) {
        // If user provided a vendor ID
        if (providedVendorId != null && !providedVendorId.trim().isEmpty()) {
            String trimmedId = providedVendorId.trim();
            
            // Check if the provided vendor ID already exists
            if (vendorRepository.existsByVendorIdAndCompanyId(trimmedId, companyId)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, 
                    "Vendor ID '" + trimmedId + "' already exists"
                );
            }
            
            return trimmedId;
        }
        
        // Otherwise, generate a new unique vendor ID
        return generateUniqueVendorId(companyId);
    }

    private String generateUniqueVendorId(Long companyId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD

        for (int attempt = 0; attempt < 30; attempt++) {
            int rand = ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("VND-%s-%04d", datePart, rand);
            if (!vendorRepository.existsByVendorIdAndCompanyId(candidate, companyId)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate unique Vendor ID");
    }

    private VendorResponse toResponse(Vendor v) {
        return VendorResponse.builder()
                .id(v.getId())
                .vendorId(v.getVendorId())
                .vendorName(v.getVendorName())
                .taxId(v.getTaxId())
                .address(v.getAddress())
                .contactPerson(v.getContactPerson())
                .email(v.getEmail())
                .phone(v.getPhone())
                .paymentTerms(v.getPaymentTerms())
                .rating(v.getRating())
                .status(v.getStatus())
                .rejectionComment(v.getRejectionComment())
                .active(v.isActive())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private String normalizeAndEnsureUniqueTaxId(String taxId, Long currentId, Long companyId) {
        if (taxId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taxId is required");
        }
        String trimmed = taxId.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taxId cannot be blank");
        }
        ensureTaxIdUnique(trimmed, currentId, companyId);
        return trimmed;
    }

    private void ensureTaxIdUnique(String taxId, Long currentId, Long companyId) {
        vendorRepository.findByTaxIdIgnoreCaseAndCompanyId(taxId, companyId).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tax ID already exists");
            }
        });
    }
}
