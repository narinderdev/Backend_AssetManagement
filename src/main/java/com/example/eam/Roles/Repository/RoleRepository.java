package com.example.eam.Roles.Repository;

import com.example.eam.Roles.Entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndCompanyId(String name, Long companyId);
    Optional<Role> findByNameIgnoreCase(String name);
    Optional<Role> findFirstByNameIgnoreCaseAndActiveTrueOrderByIdAsc(String name);
    Optional<Role> findFirstByNameIgnoreCaseAndCompanyIdOrderByIdAsc(String name, Long companyId);
    Optional<Role> findFirstByNameIgnoreCaseAndCompanyIdAndActiveTrueOrderByIdAsc(String name, Long companyId);
    Optional<Role> findFirstByNameIgnoreCaseAndCompanyIdIsNullOrderByIdAsc(String name);
    Optional<Role> findFirstByTechnicianRoleTrueAndActiveTrue();
    Optional<Role> findByIdAndActiveTrue(Long id);
    Optional<Role> findByIdAndActiveTrueAndCompanyId(Long id, Long companyId);
    Page<Role> findByActiveTrue(Pageable pageable);
    Page<Role> findByActiveTrueAndCompanyId(Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = "permissions")
    List<Role> findByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "permissions")
    List<Role> findByActiveTrueAndCompanyIdOrderByNameAsc(Long companyId);
}
