-- Create asset_categories table (id + unique name)
IF OBJECT_ID('dbo.asset_categories', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.asset_categories (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        name NVARCHAR(255) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_asset_categories_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_asset_categories_updated_at DEFAULT SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_asset_categories_name' AND object_id = OBJECT_ID('dbo.asset_categories'))
BEGIN
    CREATE UNIQUE INDEX ux_asset_categories_name ON dbo.asset_categories(name);
END;

-- If table was pre-created (e.g. by Hibernate), ensure defaults exist for timestamp columns.
IF COL_LENGTH('dbo.asset_categories', 'created_at') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.default_constraints dc
       INNER JOIN sys.columns c
           ON c.default_object_id = dc.object_id
       WHERE dc.parent_object_id = OBJECT_ID('dbo.asset_categories')
         AND c.name = 'created_at'
   )
BEGIN
    ALTER TABLE dbo.asset_categories
    ADD CONSTRAINT df_asset_categories_created_at DEFAULT SYSUTCDATETIME() FOR created_at;
END;

IF COL_LENGTH('dbo.asset_categories', 'updated_at') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.default_constraints dc
       INNER JOIN sys.columns c
           ON c.default_object_id = dc.object_id
       WHERE dc.parent_object_id = OBJECT_ID('dbo.asset_categories')
         AND c.name = 'updated_at'
   )
BEGIN
    ALTER TABLE dbo.asset_categories
    ADD CONSTRAINT df_asset_categories_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;
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

INSERT INTO dbo.asset_categories (name, created_at, updated_at)
SELECT s.name, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM @seed s
WHERE NOT EXISTS (SELECT 1 FROM dbo.asset_categories ac WHERE ac.name = s.name);

-- Legacy data migration for assets table (skip safely when base table is absent).
IF OBJECT_ID('dbo.assets', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.assets', 'asset_category_id') IS NULL
    BEGIN
        ALTER TABLE dbo.assets ADD asset_category_id BIGINT NULL;
    END;

    INSERT INTO dbo.asset_categories (name, created_at, updated_at)
    SELECT DISTINCT LTRIM(RTRIM(a.asset_category)), SYSUTCDATETIME(), SYSUTCDATETIME()
    FROM dbo.assets a
    WHERE a.asset_category IS NOT NULL
      AND LEN(LTRIM(RTRIM(a.asset_category))) > 0
      AND NOT EXISTS (
          SELECT 1 FROM dbo.asset_categories ac WHERE ac.name = LTRIM(RTRIM(a.asset_category))
      );

    UPDATE a
    SET asset_category_id = ac.id
    FROM dbo.assets a
    JOIN dbo.asset_categories ac ON ac.name = LTRIM(RTRIM(a.asset_category))
    WHERE a.asset_category IS NOT NULL
      AND a.asset_category_id IS NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_assets_asset_category')
    BEGIN
        ALTER TABLE dbo.assets
        ADD CONSTRAINT fk_assets_asset_category FOREIGN KEY (asset_category_id) REFERENCES dbo.asset_categories(id);
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_assets_asset_category_id' AND object_id = OBJECT_ID('dbo.assets'))
    BEGIN
        CREATE INDEX idx_assets_asset_category_id ON dbo.assets(asset_category_id);
    END;
END;
