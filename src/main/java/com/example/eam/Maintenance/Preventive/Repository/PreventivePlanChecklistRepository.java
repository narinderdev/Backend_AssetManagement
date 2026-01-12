package com.example.eam.Maintenance.Preventive.Repository;

import com.example.eam.Maintenance.Preventive.Entity.PreventivePlan;
import com.example.eam.Maintenance.Preventive.Entity.PreventivePlanChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreventivePlanChecklistRepository extends JpaRepository<PreventivePlanChecklistItem, Long> {
    List<PreventivePlanChecklistItem> findByPlan_Id(Long planId);
    void deleteByPlan(PreventivePlan plan);
}
