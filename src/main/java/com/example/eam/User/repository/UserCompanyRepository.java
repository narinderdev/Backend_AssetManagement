package com.example.eam.User.repository;

import com.example.eam.CompanyManagement.Entity.Company;
import com.example.eam.User.entity.UserCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCompanyRepository extends JpaRepository<UserCompany, Long> {

    boolean existsByUser_IdAndCompany_Id(Long userId, Long companyId);

    List<UserCompany> findByUser_Id(Long userId);

    List<UserCompany> findByUser_IdAndCompany_ActiveTrue(Long userId);

    @Query("""
            select case when count(uc) > 0 then true else false end
            from UserCompany uc
            where lower(uc.user.email) = lower(:email)
              and uc.user.deleted = false
              and uc.company.id = :companyId
              and uc.company.active = true
            """)
    boolean existsActiveMapping(@Param("email") String email, @Param("companyId") Long companyId);

    @Query("""
            select uc.company
            from UserCompany uc
            where lower(uc.user.email) = lower(:email)
              and uc.user.deleted = false
              and (:includeInactive = true or uc.company.active = true)
            """)
    Page<Company> findCompaniesByUserEmail(@Param("email") String email,
                                           @Param("includeInactive") boolean includeInactive,
                                           Pageable pageable);

    @Query("""
            select uc.company
            from UserCompany uc
            where lower(uc.user.email) = lower(:email)
              and uc.user.deleted = false
              and uc.company.id = :companyId
              and uc.company.active = true
            """)
    Optional<Company> findActiveCompanyByUserEmailAndCompanyId(@Param("email") String email,
                                                                @Param("companyId") Long companyId);

    void deleteByUser_Id(Long userId);
}
