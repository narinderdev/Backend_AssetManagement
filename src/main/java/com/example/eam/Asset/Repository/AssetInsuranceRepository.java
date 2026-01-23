package com.example.eam.Asset.Repository;


import com.example.eam.Asset.Entity.AssetInsurance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetInsuranceRepository extends JpaRepository<AssetInsurance, Long> {

    Optional<AssetInsurance> findByAsset_Id(Long assetId);
}
