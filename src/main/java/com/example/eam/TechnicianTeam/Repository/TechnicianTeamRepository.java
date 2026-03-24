package com.example.eam.TechnicianTeam.Repository;

import com.example.eam.TechnicianTeam.Entity.TechnicianTeam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianTeamRepository extends JpaRepository<TechnicianTeam, Long> {

    boolean existsByTeamNameIgnoreCase(String teamName);
    boolean existsByTeamNameIgnoreCaseAndCompanyId(String teamName, Long companyId);

    boolean existsByTeamNameIgnoreCaseAndIdNot(String teamName, Long id);
    boolean existsByTeamNameIgnoreCaseAndCompanyIdAndIdNot(String teamName, Long companyId, Long id);

    java.util.Optional<TechnicianTeam> findByIdAndCompanyId(Long id, Long companyId);
    org.springframework.data.domain.Page<TechnicianTeam> findByCompanyId(Long companyId, org.springframework.data.domain.Pageable pageable);
}
