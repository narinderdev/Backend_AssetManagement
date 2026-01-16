package com.example.eam.Maintenance.Predictive.Repository;

import com.example.eam.Maintenance.Predictive.Entity.AssetThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetThresholdRepository extends JpaRepository<AssetThreshold, Long> {
    Optional<AssetThreshold> findByAsset_IdAndMeterType(Long assetId, com.example.eam.Enum.MeterType meterType);

    Optional<AssetThreshold> findByAsset_Id(Long assetId);
}
