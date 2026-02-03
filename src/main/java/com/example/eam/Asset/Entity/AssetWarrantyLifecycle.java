package com.example.eam.Asset.Entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "asset_warranty_lifecycle")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetWarrantyLifecycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "asset_id", nullable = false, unique = true)
    private Asset asset;

    @Column(name = "commissioning_date")
    private LocalDate commissioningDate;

    @Column(name = "warranty_start")
    private LocalDate warrantyStart;

    @Column(name = "warranty_end") // second "Warranty Start" treated as end date
    private LocalDate warrantyEnd;

    @Column(name = "warranty_provider", length = 255)
    private String warrantyProvider;

    @Column(name = "service_contract", length = 255)
    private String serviceContract;

    @Column(name = "planned_replacement_date")
    private LocalDate plannedReplacementDate;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_planned_maintenance")
    private LocalDate nextPlannedMaintenance;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetWarrantyLifecycle that = (AssetWarrantyLifecycle) o;
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
