package com.example.eam.Maintenance.Predictive.Repository;

import com.example.eam.Maintenance.Predictive.Entity.AssetThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetThresholdRepository extends JpaRepository<AssetThreshold, Long> {
    Optional<AssetThreshold> findByAsset_IdAndMeterType(Long assetId, com.example.eam.Enum.MeterType meterType);
    Optional<AssetThreshold> findByAsset_IdAndMeterTypeAndAsset_CompanyId(Long assetId, com.example.eam.Enum.MeterType meterType, Long companyId);

    Optional<AssetThreshold> findByAsset_Id(Long assetId);
    Optional<AssetThreshold> findByAsset_IdAndAsset_CompanyId(Long assetId, Long companyId);
    Optional<AssetThreshold> findByIdAndAsset_CompanyId(Long id, Long companyId);
    org.springframework.data.domain.Page<AssetThreshold> findByAsset_CompanyId(Long companyId, org.springframework.data.domain.Pageable pageable);
}
