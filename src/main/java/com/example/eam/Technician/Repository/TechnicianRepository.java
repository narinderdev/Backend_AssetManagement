package com.example.eam.Technician.Repository;

import com.example.eam.Technician.Entity.Technician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    Optional<Technician> findByIdAndIsDeletedFalse(Long id);

    Page<Technician> findByIsDeletedFalse(Pageable pageable);

    boolean existsByEmailIgnoreCaseAndIsDeletedFalse(String email);

    Optional<Technician> findByEmailIgnoreCaseAndIsDeletedFalse(String email);

    boolean existsByBadgeNumberIgnoreCaseAndIsDeletedFalse(String badgeNumber);

    boolean existsByTechnicianIdIgnoreCaseAndIsDeletedFalse(String technicianId);

    List<Technician> findByIdInAndIsDeletedFalse(Iterable<Long> ids);

    /**
     * Backward-compatible helpers used in older code/tests. They delegate to the
     * soft-delete aware queries to avoid returning deleted technicians.
     */
    default boolean existsByEmailIgnoreCase(String email) {
        return existsByEmailIgnoreCaseAndIsDeletedFalse(email);
    }

    default Optional<Technician> findByEmailIgnoreCase(String email) {
        return findByEmailIgnoreCaseAndIsDeletedFalse(email);
    }
}
