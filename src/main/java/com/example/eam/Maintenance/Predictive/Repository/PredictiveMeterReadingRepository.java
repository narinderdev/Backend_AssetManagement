package com.example.eam.Maintenance.Predictive.Repository;

import com.example.eam.Maintenance.Predictive.Entity.PredictiveMeterReading;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictiveMeterReadingRepository extends JpaRepository<PredictiveMeterReading, Long> {

    java.util.List<PredictiveMeterReading> findByThreshold_Id(Long thresholdId);

    java.util.List<PredictiveMeterReading> findByThreshold_IdIn(java.util.Collection<Long> thresholdIds);

    void deleteByThreshold_Id(Long thresholdId);
}
