package com.example.eam.TechnicianTeam.Repository;

import com.example.eam.TechnicianTeam.Entity.TechnicianTeam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianTeamRepository extends JpaRepository<TechnicianTeam, Long> {

    boolean existsByTeamNameIgnoreCase(String teamName);

    boolean existsByTeamNameIgnoreCaseAndIdNot(String teamName, Long id);
}
