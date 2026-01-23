-- Asset types to standardize asset classification and defaults
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'asset_types')
BEGIN
    CREATE TABLE asset_types (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(64) NOT NULL,
        name NVARCHAR(255) NOT NULL,
        asset_category_id BIGINT NULL,
        default_criticality NVARCHAR(32) NULL,
        default_gl_account NVARCHAR(255) NULL,
        insurance_required BIT NOT NULL CONSTRAINT df_asset_types_insurance_required DEFAULT 0,
        active BIT NOT NULL CONSTRAINT df_asset_types_active DEFAULT 1,
        created_at DATETIME2 NOT NULL CONSTRAINT df_asset_types_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_asset_types_updated_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_asset_types_code' AND object_id = OBJECT_ID('asset_types'))
BEGIN
    CREATE UNIQUE INDEX ux_asset_types_code ON asset_types(code);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_asset_types_name' AND object_id = OBJECT_ID('asset_types'))
BEGIN
    CREATE UNIQUE INDEX ux_asset_types_name ON asset_types(name);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_asset_types_asset_category')
BEGIN
    ALTER TABLE asset_types
        ADD CONSTRAINT fk_asset_types_asset_category FOREIGN KEY (asset_category_id) REFERENCES asset_categories(id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'chk_asset_types_default_criticality')
BEGIN
    ALTER TABLE asset_types
        ADD CONSTRAINT chk_asset_types_default_criticality CHECK (default_criticality IN ('LOW','MEDIUM','HIGH','CRITICAL') OR default_criticality IS NULL);
END;
GO

-- Link assets to asset types (nullable to support existing data)
IF COL_LENGTH('assets', 'asset_type_id') IS NULL
BEGIN
    ALTER TABLE assets ADD asset_type_id BIGINT NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_assets_asset_type')
BEGIN
    ALTER TABLE assets
        ADD CONSTRAINT fk_assets_asset_type FOREIGN KEY (asset_type_id) REFERENCES asset_types(id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_assets_asset_type_id' AND object_id = OBJECT_ID('assets'))
BEGIN
    CREATE INDEX idx_assets_asset_type_id ON assets(asset_type_id);
END;
GO
