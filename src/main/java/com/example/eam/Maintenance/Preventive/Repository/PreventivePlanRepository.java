package com.example.eam.Maintenance.Preventive.Repository;

import com.example.eam.Maintenance.Preventive.Entity.PreventivePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreventivePlanRepository extends JpaRepository<PreventivePlan, Long> {
    boolean existsByPlanCode(String planCode);
    boolean existsByPlanCodeAndCompanyId(String planCode, Long companyId);
    Optional<PreventivePlan> findByIdAndDeletedFalse(Long id);
    Optional<PreventivePlan> findByIdAndDeletedFalseAndCompanyId(Long id, Long companyId);
    Page<PreventivePlan> findByDeletedFalse(Pageable pageable);
    Page<PreventivePlan> findByDeletedFalseAndCompanyId(Long companyId, Pageable pageable);
    java.util.List<PreventivePlan> findByDeletedFalseAndActiveTrueAndCompanyId(Long companyId);
}
