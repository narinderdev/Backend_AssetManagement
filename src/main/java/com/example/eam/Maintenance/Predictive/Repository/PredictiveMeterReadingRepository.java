package com.example.eam.Maintenance.Predictive.Repository;

import com.example.eam.Maintenance.Predictive.Entity.PredictiveMeterReading;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictiveMeterReadingRepository extends JpaRepository<PredictiveMeterReading, Long> {
}
