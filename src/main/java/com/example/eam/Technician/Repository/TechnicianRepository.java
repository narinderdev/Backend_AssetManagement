package com.example.eam.Technician.Repository;

import com.example.eam.Technician.Entity.Technician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    Optional<Technician> findByIdAndIsDeletedFalse(Long id);
    Optional<Technician> findByIdAndIsDeletedFalseAndCompanyId(Long id, Long companyId);

    Page<Technician> findByIsDeletedFalse(Pageable pageable);
    Page<Technician> findByIsDeletedFalseAndCompanyId(Long companyId, Pageable pageable);

    boolean existsByEmailIgnoreCaseAndIsDeletedFalse(String email);
    boolean existsByEmailIgnoreCaseAndIsDeletedFalseAndCompanyId(String email, Long companyId);

    Optional<Technician> findByEmailIgnoreCaseAndIsDeletedFalse(String email);
    Optional<Technician> findByEmailIgnoreCaseAndIsDeletedFalseAndCompanyId(String email, Long companyId);

    boolean existsByBadgeNumberIgnoreCaseAndIsDeletedFalse(String badgeNumber);
    boolean existsByBadgeNumberIgnoreCaseAndIsDeletedFalseAndCompanyId(String badgeNumber, Long companyId);

    boolean existsByTechnicianIdIgnoreCaseAndIsDeletedFalse(String technicianId);
    boolean existsByTechnicianIdIgnoreCaseAndIsDeletedFalseAndCompanyId(String technicianId, Long companyId);

    Optional<Technician> findByTechnicianIdIgnoreCaseAndIsDeletedFalse(String technicianId);
    Optional<Technician> findByTechnicianIdIgnoreCaseAndIsDeletedFalseAndCompanyId(String technicianId, Long companyId);

    Optional<Technician> findFirstByTechnicianIdIgnoreCase(String technicianId);

    Optional<Technician> findFirstByEmailIgnoreCase(String email);

    List<Technician> findByIdInAndIsDeletedFalse(Iterable<Long> ids);
    List<Technician> findByIdInAndIsDeletedFalseAndCompanyId(Iterable<Long> ids, Long companyId);

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
