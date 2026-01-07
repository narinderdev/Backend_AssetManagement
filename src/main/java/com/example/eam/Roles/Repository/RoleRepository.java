package com.example.eam.Roles.Repository;

import com.example.eam.Roles.Entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Role> findByNameIgnoreCase(String name);
    Optional<Role> findByIdAndActiveTrue(Long id);
    Page<Role> findByActiveTrue(Pageable pageable);
}

