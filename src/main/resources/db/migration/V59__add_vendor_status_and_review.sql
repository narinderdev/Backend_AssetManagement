-- Add status + rejection_comment columns to vendors for approval workflow
IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL AND COL_LENGTH('dbo.vendors', 'status') IS NULL
BEGIN
    ALTER TABLE dbo.vendors ADD status NVARCHAR(32) NULL;
END;
GO
IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL AND COL_LENGTH('dbo.vendors', 'rejection_comment') IS NULL
BEGIN
    ALTER TABLE dbo.vendors ADD rejection_comment NVARCHAR(1000) NULL;
END;
GO

-- Backfill existing vendors as approved to preserve current behaviour
IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL
BEGIN
    UPDATE dbo.vendors
    SET status = 'APPROVED'
    WHERE status IS NULL;
END;
GO

-- Enforce NOT NULL and default
IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL
   AND EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.vendors')
      AND name = 'status'
      AND is_nullable = 1
)
BEGIN
    ALTER TABLE dbo.vendors ALTER COLUMN status NVARCHAR(32) NOT NULL;
END;
GO

IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL
   AND NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints dc
        JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
    WHERE dc.parent_object_id = OBJECT_ID('dbo.vendors')
      AND c.name = 'status'
)
BEGIN
    ALTER TABLE dbo.vendors ADD CONSTRAINT df_vendors_status DEFAULT 'PENDING' FOR status;
END;
GO

-- Index to speed up filtering on approved & active
IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_vendors_status_active'
         AND object_id = OBJECT_ID('dbo.vendors')
   )
BEGIN
    CREATE INDEX idx_vendors_status_active ON dbo.vendors(status, active);
END;
GO

