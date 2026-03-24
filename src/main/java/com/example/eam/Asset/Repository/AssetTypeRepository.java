package com.example.eam.Asset.Repository;


import com.example.eam.Asset.Entity.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetTypeRepository extends JpaRepository<AssetType, Long> {

    List<AssetType> findAllByCompanyIdOrderByNameAsc(Long companyId);

    List<AssetType> findAllByCompanyIdAndActiveTrueOrderByNameAsc(Long companyId);

    Optional<AssetType> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCodeIgnoreCaseAndCompanyId(String code, Long companyId);

    boolean existsByNameIgnoreCaseAndCompanyId(String name, Long companyId);

    boolean existsByCodeIgnoreCaseAndCompanyIdAndIdNot(String code, Long companyId, Long id);

    boolean existsByNameIgnoreCaseAndCompanyIdAndIdNot(String name, Long companyId, Long id);
}
