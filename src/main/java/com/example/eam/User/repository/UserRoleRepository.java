package com.example.eam.User.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.eam.User.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    void deleteByUserId(Long userId);
    boolean existsByUser_IdAndRole_Id(Long userId, Long roleId);

    long countByRole_NameIgnoreCaseAndUser_DeletedFalse(String roleName);

    long countByRole_NameIgnoreCaseAndUser_IdNotAndUser_DeletedFalse(String roleName, Long userId);

    @Query("""
            select count(ur)
            from UserRole ur
            join ur.user u
            join u.userCompanies uc
            where lower(ur.role.name) = lower(:roleName)
              and u.deleted = false
              and uc.company.id = :companyId
              and uc.company.active = true
            """)
    long countByRoleNameIgnoreCaseAndCompanyIdAndUserDeletedFalse(@Param("roleName") String roleName,
                                                                  @Param("companyId") Long companyId);

    @Query("""
            select count(ur)
            from UserRole ur
            join ur.user u
            join u.userCompanies uc
            where lower(ur.role.name) = lower(:roleName)
              and u.id <> :userId
              and u.deleted = false
              and uc.company.id = :companyId
              and uc.company.active = true
            """)
    long countByRoleNameIgnoreCaseAndCompanyIdAndUserIdNotAndUserDeletedFalse(@Param("roleName") String roleName,
                                                                               @Param("companyId") Long companyId,
                                                                               @Param("userId") Long userId);
}
