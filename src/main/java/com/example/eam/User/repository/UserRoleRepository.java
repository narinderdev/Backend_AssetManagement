package com.example.eam.User.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.eam.User.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    void deleteByUserId(Long userId);
}
