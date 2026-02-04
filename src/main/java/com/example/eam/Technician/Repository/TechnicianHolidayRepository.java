package com.example.eam.Technician.Repository;

import com.example.eam.Technician.Entity.TechnicianHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TechnicianHolidayRepository extends JpaRepository<TechnicianHoliday, Long> {

    boolean existsByHolidayDate(LocalDate holidayDate);
    boolean existsByHolidayDateAndIdNot(LocalDate holidayDate, Long id);

    @Query("""
        select th from TechnicianHoliday th
        where th.holidayDate >= :rangeStart
          and th.holidayDate < :rangeEnd
        order by th.holidayDate asc
    """)
    List<TechnicianHoliday> findInRange(
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd
    );

    List<TechnicianHoliday> findAllByOrderByHolidayDateAsc();
}
