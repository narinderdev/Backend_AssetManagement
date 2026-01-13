package com.example.eam.Maintenance.Emergency.Repository;

import com.example.eam.Maintenance.Emergency.Entity.EmergencyIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmergencyIncidentRepository extends JpaRepository<EmergencyIncident, Long> {
    Optional<EmergencyIncident> findByWorkOrder_Id(Long workOrderId);
}
