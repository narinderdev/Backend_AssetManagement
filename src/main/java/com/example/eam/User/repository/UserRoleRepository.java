package com.example.eam.User.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.eam.User.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    void deleteByUserId(Long userId);

    long countByRole_NameIgnoreCaseAndUser_DeletedFalse(String roleName);

    long countByRole_NameIgnoreCaseAndUser_IdNotAndUser_DeletedFalse(String roleName, Long userId);
}
