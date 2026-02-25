package com.example.eam.VendorManagement.Entity;

import com.example.eam.Enum.PaymentTerms;
import com.example.eam.Enum.VendorStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vendors",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vendors_vendor_id", columnNames = "vendor_id"),
                @UniqueConstraint(name = "uk_vendors_tax_id", columnNames = "tax_id")
        },
        indexes = {
                @Index(name = "idx_vendors_vendor_id", columnList = "vendor_id"),
                @Index(name = "idx_vendors_active", columnList = "active"),
                @Index(name = "idx_vendors_tax_id", columnList = "tax_id"),
                @Index(name = "idx_vendors_status_active", columnList = "status, active")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB PK

    @Column(name = "vendor_id", nullable = false, unique = true, length = 64)
    private String vendorId; // Business ID (auto-generated)

    @Column(name = "vendor_name", nullable = false, length = 255)
    private String vendorName;

    @Column(name = "tax_id", nullable = false, length = 128)
    private String taxId;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "contact_person", nullable = false, length = 255)
    private String contactPerson;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 60)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", length = 32)
    private PaymentTerms paymentTerms;

    @Column(name = "rating")
    private Integer rating; // 1-5 optional

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private VendorStatus status = VendorStatus.PENDING;

    @Column(name = "rejection_comment", length = 1000)
    private String rejectionComment;

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

