-- Create asset_categories table (id + unique name)
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'asset_categories')
BEGIN
    CREATE TABLE asset_categories (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        name NVARCHAR(255) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_asset_categories_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_asset_categories_updated_at DEFAULT SYSUTCDATETIME()
    );

    CREATE UNIQUE INDEX ux_asset_categories_name ON asset_categories(name);
END;

-- Seed default categories (idempotent)
DECLARE @seed TABLE(name NVARCHAR(255));
INSERT INTO @seed (name) VALUES
 (N'Electrical'),
 (N'Mechanical'),
 (N'HVAC'),
 (N'Plumbing & Water Systems'),
 (N'Fire & Safety Systems'),
 (N'Building / Civil'),
 (N'IT & Network'),
 (N'Vehicles & Fleet'),
 (N'Production / Plant Equipment'),
 (N'Tools & Instruments'),
 (N'Utilities'),
 (N'Security Systems'),
 (N'Renewable Energy'),
 (N'Medical Equipment'),
 (N'Furniture & Fixtures');

INSERT INTO asset_categories (name)
SELECT s.name
FROM @seed s
WHERE NOT EXISTS (SELECT 1 FROM asset_categories ac WHERE ac.name = s.name);

-- Add FK column on assets if missing
IF COL_LENGTH('assets', 'asset_category_id') IS NULL
BEGIN
    ALTER TABLE assets ADD asset_category_id BIGINT NULL;
END;

-- Backfill categories from existing asset rows
INSERT INTO asset_categories (name)
SELECT DISTINCT LTRIM(RTRIM(a.asset_category))
FROM assets a
WHERE a.asset_category IS NOT NULL
  AND LEN(LTRIM(RTRIM(a.asset_category))) > 0
  AND NOT EXISTS (
      SELECT 1 FROM asset_categories ac WHERE ac.name = LTRIM(RTRIM(a.asset_category))
  );

-- Link assets to categories
UPDATE a
SET asset_category_id = ac.id
FROM assets a
JOIN asset_categories ac ON ac.name = LTRIM(RTRIM(a.asset_category))
WHERE a.asset_category IS NOT NULL
  AND a.asset_category_id IS NULL;

-- Foreign key & index for faster lookups
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_assets_asset_category')
BEGIN
    ALTER TABLE assets
    ADD CONSTRAINT fk_assets_asset_category FOREIGN KEY (asset_category_id) REFERENCES asset_categories(id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_assets_asset_category_id' AND object_id = OBJECT_ID('assets'))
BEGIN
    CREATE INDEX idx_assets_asset_category_id ON assets(asset_category_id);
END;
