package com.example.eam.Technician.Entity;

import com.example.eam.Enum.TechnicianHolidayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "technician_holidays",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_technician_holidays_company_date", columnNames = {"company_id", "holiday_date"})
        },
        indexes = {
                @Index(name = "idx_technician_holidays_type", columnList = "holiday_type"),
                @Index(name = "idx_technician_holidays_company", columnList = "company_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "holiday_name", nullable = false, length = 255)
    private String holidayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false, length = 32)
    private TechnicianHolidayType holidayType;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "notes", length = 512)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
