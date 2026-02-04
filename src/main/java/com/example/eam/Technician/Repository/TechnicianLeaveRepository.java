package com.example.eam.Technician.Repository;

import com.example.eam.Technician.Entity.TechnicianLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TechnicianLeaveRepository extends JpaRepository<TechnicianLeave, Long> {

    boolean existsByTechnician_IdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(Long technicianId,
                                                                                    LocalDate startDate,
                                                                                    LocalDate endDate);

    @Query("""
        select tl from TechnicianLeave tl
        where tl.technician.id = :technicianId
          and tl.endDate >= :rangeStart
          and tl.startDate < :rangeEnd
        order by tl.startDate asc
    """)
    List<TechnicianLeave> findOverlapping(
            @Param("technicianId") Long technicianId,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd
    );

    @Query("""
        select count(distinct tl.technician.id)
        from TechnicianLeave tl
        where tl.startDate <= :date
          and tl.endDate >= :date
    """)
    long countTechniciansOnLeave(@Param("date") LocalDate date);
}
