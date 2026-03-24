package com.example.eam.User.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.eam.User.entity.Users;

import java.util.List;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Users> findByEmailAndDeletedFalse(String email);
    Optional<Users> findByIdAndDeletedFalse(Long id);

    @Query("""
        select distinct u from Users u
        left join fetch u.userRoles ur
        left join fetch ur.role r
        where u.deleted = false
    """)
    List<Users> findAllActiveWithRoles();

    @Query("""
        select distinct u from Users u
        join u.userCompanies uc
        left join fetch u.userRoles ur
        left join fetch ur.role r
        where u.deleted = false
          and uc.company.id = :companyId
          and uc.company.active = true
    """)
    List<Users> findAllActiveWithRolesByCompanyId(@Param("companyId") Long companyId);
}


