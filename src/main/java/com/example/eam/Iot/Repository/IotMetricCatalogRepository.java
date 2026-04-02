package com.example.eam.Iot.Repository;

import com.example.eam.Iot.Entity.IotMetricCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IotMetricCatalogRepository extends JpaRepository<IotMetricCatalog, Long> {

    Optional<IotMetricCatalog> findByIdAndCompanyId(Long id, Long companyId);

    Optional<IotMetricCatalog> findByCompanyIdAndMetricCode(Long companyId, String metricCode);

    boolean existsByCompanyIdAndMetricCode(Long companyId, String metricCode);

    boolean existsByCompanyIdAndMetricCodeAndIdNot(Long companyId, String metricCode, Long id);

    Page<IotMetricCatalog> findByCompanyId(Long companyId, Pageable pageable);
}

