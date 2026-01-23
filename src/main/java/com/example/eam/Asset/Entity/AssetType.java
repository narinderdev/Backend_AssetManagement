package com.example.eam.Asset.Entity;


import com.example.eam.Enum.AssetCriticality;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "asset_types",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_asset_types_code", columnNames = "code"),
                @UniqueConstraint(name = "ux_asset_types_name", columnNames = "name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_category_id")
    private AssetCategory assetCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_criticality", length = 32)
    private AssetCriticality defaultCriticality;

    @Column(name = "default_gl_account", length = 255)
    private String defaultGlAccount;

    @Column(name = "insurance_required", nullable = false)
    private Boolean insuranceRequired;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetType that = (AssetType) o;
        if (this.id != null && that.id != null) {
            return this.id.equals(that.id);
        }
        return this.code != null && that.code != null && this.code.equalsIgnoreCase(that.code);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
