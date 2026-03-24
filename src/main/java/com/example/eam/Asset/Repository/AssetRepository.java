package com.example.eam.Asset.Repository;


import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsByAssetId(String assetId);
    Optional<Asset> findByIdAndCompanyId(Long id, Long companyId);
    Page<Asset> findByCompanyId(Long companyId, Pageable pageable);

    List<Asset> findByAssetCategory_Name(String assetCategory);

    long countByCriticalityAndStatusIn(AssetCriticality criticality, Collection<AssetStatus> statuses);

    List<Asset> findByAssetTypeRef_Id(Long assetTypeId);

    boolean existsByAssetTypeRef_Id(Long assetTypeId);

    @Query("""
        select a from Asset a
        left join a.warrantyLifecycle wl
        left join a.assetTypeRef at
        where a.status in :statuses
          and (:companyId is null or a.companyId = :companyId)
          and (:criticality is null or a.criticality = :criticality)
          and (:assetTypeId is null or at.id = :assetTypeId)
          and (:warrantyStart is null or (wl.warrantyEnd is not null and wl.warrantyEnd between :warrantyStart and :warrantyEnd))
    """)
    Page<Asset> findForReport(@Param("statuses") Collection<AssetStatus> statuses,
                              @Param("companyId") Long companyId,
                              @Param("criticality") AssetCriticality criticality,
                              @Param("assetTypeId") Long assetTypeId,
                              @Param("warrantyStart") LocalDate warrantyStart,
                              @Param("warrantyEnd") LocalDate warrantyEnd,
                              Pageable pageable);
}
