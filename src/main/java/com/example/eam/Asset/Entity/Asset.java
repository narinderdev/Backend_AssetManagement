package com.example.eam.Asset.Entity;

import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Business key shown to user: "Asset ID*"
    @Column(name = "asset_code", nullable = false, unique = true, length = 64)
    private String assetId;

    @Column(name = "asset_name", nullable = false, length = 255)
    private String assetName;

    @Column(name = "short_description", length = 1000)
    private String shortDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_category_id")
    private AssetCategory assetCategory;

    @Column(name = "asset_category", nullable = false, length = 128)
    private String assetCategoryName;

    @Column(name = "asset_type", length = 128)
    private String assetType;

    // // Parent Asset: hierarchical relationship
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "parent_asset_id")
    // private Asset parentAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AssetStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "criticality", length = 32)
    private AssetCriticality criticality;

    @Column(name = "ownership", length = 64)
    private String ownership; // e.g. Owned, Leased, Rented

    @Column(name = "asset_tag", length = 128)
    private String assetTag; // Tag / Barcode / RFID

    // 1:1 sections
    @OneToOne(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AssetLocation location;

    @OneToOne(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AssetTechnicalDetails technicalDetails;

    @OneToOne(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AssetFinancialDetails financialDetails;

    @OneToOne(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AssetWarrantyLifecycle warrantyLifecycle;

    @OneToOne(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AssetSafetyOperations safetyOperations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        if (this.id != null && asset.id != null) {
            return this.id.equals(asset.id);
        }
        return this.assetId != null && this.assetId.equals(asset.assetId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
