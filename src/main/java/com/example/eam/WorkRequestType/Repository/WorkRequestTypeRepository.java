package com.example.eam.WorkRequestType.Repository;

import com.example.eam.WorkRequestType.Entity.WorkRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkRequestTypeRepository extends JpaRepository<WorkRequestType, Long> {

    Optional<WorkRequestType> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<WorkRequestType> findAllByOrderByCodeAsc();
}
