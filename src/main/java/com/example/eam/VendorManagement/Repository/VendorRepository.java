package com.example.eam.VendorManagement.Repository;


import com.example.eam.VendorManagement.Entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    boolean existsByVendorId(String vendorId);
    boolean existsByTaxIdIgnoreCase(String taxId);
    Optional<Vendor> findByTaxIdIgnoreCase(String taxId);

    Optional<Vendor> findByIdAndActiveTrue(Long id);

    Page<Vendor> findByActiveTrue(Pageable pageable);
}

