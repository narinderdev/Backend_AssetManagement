package com.example.eam.Iot.Repository;

import com.example.eam.Iot.Entity.IotAlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IotAlertRuleRepository extends JpaRepository<IotAlertRule, Long> {

    Optional<IotAlertRule> findByIdAndCompanyId(Long id, Long companyId);

    Optional<IotAlertRule> findByCompanyIdAndAsset_IdAndMetric_Id(Long companyId, Long assetId, Long metricId);

    Optional<IotAlertRule> findByCompanyIdAndAsset_IdAndMetric_MetricCode(Long companyId, Long assetId, String metricCode);

    Page<IotAlertRule> findByCompanyId(Long companyId, Pageable pageable);
}

