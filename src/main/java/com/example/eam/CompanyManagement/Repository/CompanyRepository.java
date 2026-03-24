package com.example.eam.CompanyManagement.Repository;

import com.example.eam.CompanyManagement.Entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByCompanyNumberIgnoreCase(String companyNumber);

    Optional<Company> findByCompanyNumberIgnoreCase(String companyNumber);

    Optional<Company> findByIdAndActiveTrue(Long id);

    Page<Company> findByActiveTrue(Pageable pageable);

    List<Company> findByActiveTrue();
}
