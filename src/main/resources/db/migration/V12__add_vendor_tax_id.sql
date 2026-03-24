-- Add tax_id to vendors and enforce non-null + uniqueness

-- 1) Add column nullable first
IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL AND COL_LENGTH('dbo.vendors', 'tax_id') IS NULL
BEGIN
    ALTER TABLE dbo.vendors ADD tax_id NVARCHAR(128) NULL;
END;
GO

-- 2) Backfill existing rows with unique placeholder values
IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL
BEGIN
    UPDATE v
    SET tax_id = 'PENDING-' + CAST(v.id AS NVARCHAR(32))
    FROM dbo.vendors v
    WHERE v.tax_id IS NULL;
END;
GO

-- 3) Make column NOT NULL
IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL
   AND EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.vendors')
      AND name = 'tax_id'
      AND is_nullable = 1
)
BEGIN
    ALTER TABLE dbo.vendors ALTER COLUMN tax_id NVARCHAR(128) NOT NULL;
END;
GO

-- 4) Add unique index on tax_id
IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'ux_vendors_tax_id'
         AND object_id = OBJECT_ID('dbo.vendors')
   )
BEGIN
    CREATE UNIQUE INDEX ux_vendors_tax_id ON dbo.vendors(tax_id);
END;
GO

