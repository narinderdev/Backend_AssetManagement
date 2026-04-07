package com.example.eam.Asset.Entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "asset_location")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "asset_id", nullable = false, unique = true)
    private Asset asset;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "department", length = 255)
    private String department;

    @Column(name = "cost_center", length = 128)
    private String costCenter;

    @Column(name = "assigned_owner", length = 255)
    private String assignedOwner;

    @Column(name = "maintenance_team", length = 255)
    private String maintenanceTeam;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetLocation that = (AssetLocation) o;
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
