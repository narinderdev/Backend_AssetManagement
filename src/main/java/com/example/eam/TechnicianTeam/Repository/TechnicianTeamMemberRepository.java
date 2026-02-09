package com.example.eam.TechnicianTeam.Repository;

import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechnicianTeamMemberRepository extends JpaRepository<TechnicianTeamMember, Long> {

    List<TechnicianTeamMember> findByTeam_Id(Long teamId);

    List<TechnicianTeamMember> findByTechnician_Id(Long technicianId);

    boolean existsByTeam_Id(Long teamId);

    void deleteByTechnician_Id(Long technicianId);
}
