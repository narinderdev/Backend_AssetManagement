package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WoNumberPool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WoNumberPoolRepository extends JpaRepository<WoNumberPool, Long> {

    long countByAssignedFalse();
}
