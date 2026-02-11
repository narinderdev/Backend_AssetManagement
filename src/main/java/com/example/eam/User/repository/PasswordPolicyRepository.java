package com.example.eam.User.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.eam.User.entity.PasswordPolicy;

public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicy, Long> {
    Optional<PasswordPolicy> findTopByOrderByIdAsc();
}
