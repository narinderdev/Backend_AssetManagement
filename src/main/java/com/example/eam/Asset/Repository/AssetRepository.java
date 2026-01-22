package com.example.eam.Asset.Repository;


import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsByAssetId(String assetId);

    List<Asset> findByAssetCategory_Name(String assetCategory);

    long countByCriticalityAndStatusIn(AssetCriticality criticality, Collection<AssetStatus> statuses);
}
