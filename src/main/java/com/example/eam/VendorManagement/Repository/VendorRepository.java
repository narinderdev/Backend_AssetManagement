package com.example.eam.VendorManagement.Repository;


import com.example.eam.VendorManagement.Entity.Vendor;
import com.example.eam.Enum.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    boolean existsByVendorId(String vendorId);
    boolean existsByVendorIdAndCompanyId(String vendorId, Long companyId);
    boolean existsByTaxIdIgnoreCase(String taxId);
    boolean existsByTaxIdIgnoreCaseAndCompanyId(String taxId, Long companyId);
    Optional<Vendor> findByTaxIdIgnoreCase(String taxId);
    Optional<Vendor> findByTaxIdIgnoreCaseAndCompanyId(String taxId, Long companyId);

    Optional<Vendor> findByIdAndActiveTrue(Long id);
    Optional<Vendor> findByIdAndActiveTrueAndCompanyId(Long id, Long companyId);
    Optional<Vendor> findByIdAndCompanyId(Long id, Long companyId);
    Optional<Vendor> findByIdAndActiveTrueAndStatus(Long id, VendorStatus status);

    Page<Vendor> findByActiveTrue(Pageable pageable);
    Page<Vendor> findByActiveTrueAndCompanyId(Long companyId, Pageable pageable);
    Page<Vendor> findByActiveTrueAndStatus(VendorStatus status, Pageable pageable);
    Page<Vendor> findByActiveTrueAndStatusAndCompanyId(VendorStatus status, Long companyId, Pageable pageable);
    Page<Vendor> findByStatus(VendorStatus status, Pageable pageable);
    Page<Vendor> findByStatusAndCompanyId(VendorStatus status, Long companyId, Pageable pageable);
    Page<Vendor> findByStatusIn(List<VendorStatus> statuses, Pageable pageable);
    Page<Vendor> findByStatusInAndCompanyId(List<VendorStatus> statuses, Long companyId, Pageable pageable);
    Page<Vendor> findByActiveTrueAndStatusIn(List<VendorStatus> statuses, Pageable pageable);
    Page<Vendor> findByActiveTrueAndStatusInAndCompanyId(List<VendorStatus> statuses, Long companyId, Pageable pageable);
}
