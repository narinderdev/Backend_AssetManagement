package com.example.eam.CompanyManagement.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "companies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_companies_company_number", columnNames = "company_number")
        },
        indexes = {
                @Index(name = "idx_companies_company_number", columnList = "company_number"),
                @Index(name = "idx_companies_active", columnList = "active")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_legal_name", nullable = false, length = 255)
    private String companyLegalName;

    @Column(name = "company_trade_name", nullable = false, length = 255)
    private String companyTradeName;

    @Column(name = "company_number", nullable = false, length = 100)
    private String companyNumber;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "city", nullable = false, length = 150)
    private String city;

    @Column(name = "country", nullable = false, length = 150)
    private String country;

    @Column(name = "postal_code", nullable = false, length = 40)
    private String postalCode;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
