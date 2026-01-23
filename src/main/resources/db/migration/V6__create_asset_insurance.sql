-- Asset insurance table to support expiry tracking and compliance visibility
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'asset_insurance')
BEGIN
    CREATE TABLE asset_insurance (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        asset_id BIGINT NOT NULL,
        insurance_provider NVARCHAR(255) NOT NULL,
        policy_number NVARCHAR(128) NOT NULL,
        policy_start_date DATE NOT NULL,
        policy_expiry_date DATE NOT NULL,
        insurance_status NVARCHAR(32) NOT NULL,
        policy_type NVARCHAR(128) NULL,
        certificate_url NVARCHAR(512) NULL,
        coverage_amount DECIMAL(19,4) NULL,
        premium_amount DECIMAL(19,4) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_asset_insurance_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_asset_insurance_updated_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_asset_insurance_asset')
BEGIN
    ALTER TABLE asset_insurance
        ADD CONSTRAINT fk_asset_insurance_asset FOREIGN KEY (asset_id) REFERENCES assets(id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_asset_insurance_asset_id' AND object_id = OBJECT_ID('asset_insurance'))
BEGIN
    CREATE UNIQUE INDEX ux_asset_insurance_asset_id ON asset_insurance(asset_id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'chk_asset_insurance_expiry_after_start')
BEGIN
    ALTER TABLE asset_insurance
        ADD CONSTRAINT chk_asset_insurance_expiry_after_start CHECK (policy_expiry_date >= policy_start_date);
END;
GO
