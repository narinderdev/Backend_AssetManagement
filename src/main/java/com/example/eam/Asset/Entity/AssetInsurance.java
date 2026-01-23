package com.example.eam.Asset.Entity;


import com.example.eam.Enum.InsuranceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "asset_insurance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInsurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "asset_id", nullable = false, unique = true)
    private Asset asset;

    @Column(name = "insurance_provider", nullable = false, length = 255)
    private String insuranceProvider;

    @Column(name = "policy_number", nullable = false, length = 128)
    private String policyNumber;

    @Column(name = "policy_start_date", nullable = false)
    private LocalDate policyStartDate;

    @Column(name = "policy_expiry_date", nullable = false)
    private LocalDate policyExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_status", nullable = false, length = 32)
    private InsuranceStatus insuranceStatus;

    @Column(name = "policy_type", length = 128)
    private String policyType;

    @Column(name = "certificate_url", length = 512)
    private String certificateUrl;

    @Column(name = "coverage_amount", precision = 19, scale = 4)
    private BigDecimal coverageAmount;

    @Column(name = "premium_amount", precision = 19, scale = 4)
    private BigDecimal premiumAmount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetInsurance that = (AssetInsurance) o;
        if (this.id != null && that.id != null) {
            return this.id.equals(that.id);
        }
        return this.asset != null && that.asset != null && this.asset.equals(that.asset);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
