package com.example.eam.Technician.Repository;

import com.example.eam.Technician.Entity.TechnicianDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TechnicianDeviceTokenRepository extends JpaRepository<TechnicianDeviceToken, Long> {

    Optional<TechnicianDeviceToken> findByDeviceToken(String deviceToken);

    List<TechnicianDeviceToken> findByTechnician_Id(Long technicianId);

    Optional<TechnicianDeviceToken> findByTechnician_IdAndPlatform(Long technicianId, com.example.eam.Enum.DevicePlatform platform);
}
