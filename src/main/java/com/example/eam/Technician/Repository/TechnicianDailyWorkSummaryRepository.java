package com.example.eam.Technician.Repository;

import com.example.eam.Technician.Entity.TechnicianDailyWorkSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TechnicianDailyWorkSummaryRepository extends JpaRepository<TechnicianDailyWorkSummary, Long> {

    Optional<TechnicianDailyWorkSummary> findByTechnician_IdAndWorkDate(Long technicianId, LocalDate workDate);
}
