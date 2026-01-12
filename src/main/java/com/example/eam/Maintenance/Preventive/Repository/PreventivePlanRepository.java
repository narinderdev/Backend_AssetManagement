package com.example.eam.Maintenance.Preventive.Repository;

import com.example.eam.Maintenance.Preventive.Entity.PreventivePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreventivePlanRepository extends JpaRepository<PreventivePlan, Long> {
    boolean existsByPlanCode(String planCode);
    Optional<PreventivePlan> findByIdAndDeletedFalse(Long id);
    Page<PreventivePlan> findByDeletedFalse(Pageable pageable);
}
