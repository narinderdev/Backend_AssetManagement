package com.example.eam.Technician.Repository;

import com.example.eam.Technician.Entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Technician> findByEmailIgnoreCase(String email);
}
