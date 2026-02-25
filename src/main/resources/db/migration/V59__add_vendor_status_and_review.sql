-- Add status + rejection_comment columns to vendors for approval workflow

IF COL_LENGTH('vendors', 'status') IS NULL
BEGIN
    ALTER TABLE vendors ADD status NVARCHAR(32) NULL;
END;
GO

IF COL_LENGTH('vendors', 'rejection_comment') IS NULL
BEGIN
    ALTER TABLE vendors ADD rejection_comment NVARCHAR(1000) NULL;
END;
GO

-- Backfill existing vendors as approved to preserve current behaviour
UPDATE vendors
SET status = 'APPROVED'
WHERE status IS NULL;
GO

-- Enforce NOT NULL and default
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('vendors')
      AND name = 'status'
      AND is_nullable = 1
)
BEGIN
    ALTER TABLE vendors ALTER COLUMN status NVARCHAR(32) NOT NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints dc
        JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
    WHERE dc.parent_object_id = OBJECT_ID('vendors')
      AND c.name = 'status'
)
BEGIN
    ALTER TABLE vendors ADD CONSTRAINT df_vendors_status DEFAULT 'PENDING' FOR status;
END;
GO

-- Index to speed up filtering on approved & active
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_vendors_status_active' AND object_id = OBJECT_ID('vendors'))
BEGIN
    CREATE INDEX idx_vendors_status_active ON vendors(status, active);
END;
GO
