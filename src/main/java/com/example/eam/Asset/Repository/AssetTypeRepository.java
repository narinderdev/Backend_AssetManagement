package com.example.eam.Asset.Repository;


import com.example.eam.Asset.Entity.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetTypeRepository extends JpaRepository<AssetType, Long> {

    List<AssetType> findAllByActiveTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
