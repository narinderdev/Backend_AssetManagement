package com.example.eam.Technician.Repository;

import com.example.eam.Technician.Entity.TechnicianHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TechnicianHolidayRepository extends JpaRepository<TechnicianHoliday, Long> {

    boolean existsByHolidayDate(LocalDate holidayDate);
    boolean existsByHolidayDateAndCompanyId(LocalDate holidayDate, Long companyId);
    boolean existsByHolidayDateAndIdNot(LocalDate holidayDate, Long id);
    boolean existsByHolidayDateAndCompanyIdAndIdNot(LocalDate holidayDate, Long companyId, Long id);

    @Query("""
        select th from TechnicianHoliday th
        where th.holidayDate >= :rangeStart
          and th.holidayDate < :rangeEnd
          and th.companyId = :companyId
        order by th.holidayDate asc
    """)
    List<TechnicianHoliday> findInRange(
            @Param("companyId") Long companyId,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd
    );

    List<TechnicianHoliday> findAllByOrderByHolidayDateAsc();
    List<TechnicianHoliday> findAllByCompanyIdOrderByHolidayDateAsc(Long companyId);
    java.util.Optional<TechnicianHoliday> findByIdAndCompanyId(Long id, Long companyId);
}
