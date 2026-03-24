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
    boolean existsByTechnician_IdAndTechnician_CompanyIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(Long technicianId,
                                                                                                             Long companyId,
                                                                                                             LocalDate startDate,
                                                                                                             LocalDate endDate);

    boolean existsByTechnician_IdAndEndDateGreaterThanEqualAndStartDateLessThanEqualAndIdNot(Long technicianId,
                                                                                             LocalDate startDate,
                                                                                             LocalDate endDate,
                                                                                             Long idToExclude);
    boolean existsByTechnician_IdAndTechnician_CompanyIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualAndIdNot(Long technicianId,
                                                                                                                      Long companyId,
                                                                                                                      LocalDate startDate,
                                                                                                                      LocalDate endDate,
                                                                                                                      Long idToExclude);

    @Query("""
        select tl from TechnicianLeave tl
        where tl.technician.id = :technicianId
          and tl.technician.companyId = :companyId
          and tl.endDate >= :rangeStart
          and tl.startDate < :rangeEnd
        order by tl.startDate asc
    """)
    List<TechnicianLeave> findOverlapping(
            @Param("technicianId") Long technicianId,
            @Param("companyId") Long companyId,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd
    );

    @Query("""
        select count(distinct tl.technician.id)
        from TechnicianLeave tl
        where tl.startDate <= :date
          and tl.endDate >= :date
          and tl.technician.companyId = :companyId
    """)
    long countTechniciansOnLeave(@Param("date") LocalDate date, @Param("companyId") Long companyId);

    List<TechnicianLeave> findByTechnician_IdOrderByStartDateAsc(Long technicianId);
    List<TechnicianLeave> findByTechnician_IdAndTechnician_CompanyIdOrderByStartDateAsc(Long technicianId, Long companyId);

    List<TechnicianLeave> findAllByOrderByStartDateAsc();
    List<TechnicianLeave> findAllByTechnician_CompanyIdOrderByStartDateAsc(Long companyId);

    java.util.Optional<TechnicianLeave> findByIdAndTechnician_CompanyId(Long id, Long companyId);

    @Query("""
        select tl from TechnicianLeave tl
        where tl.endDate >= :rangeStart
          and tl.startDate < :rangeEnd
          and tl.technician.companyId = :companyId
        order by tl.startDate asc
    """)
    List<TechnicianLeave> findOverlappingAny(
            @Param("companyId") Long companyId,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd
    );
}
