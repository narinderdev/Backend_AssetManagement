-- Add badge number, technician id, photo, certificate info to technicians
IF COL_LENGTH('technicians', 'technician_id') IS NULL
BEGIN
    ALTER TABLE technicians ADD technician_id NVARCHAR(64) NOT NULL CONSTRAINT df_technicians_technician_id DEFAULT 'PENDING';
END;
GO

IF COL_LENGTH('technicians', 'badge_number') IS NULL
BEGIN
    ALTER TABLE technicians ADD badge_number NVARCHAR(64) NOT NULL CONSTRAINT df_technicians_badge_number DEFAULT 'PENDING';
END;
GO

IF COL_LENGTH('technicians', 'technician_photo_url') IS NULL
BEGIN
    ALTER TABLE technicians ADD technician_photo_url NVARCHAR(512) NULL;
END;
GO

IF COL_LENGTH('technicians', 'certificate_url') IS NULL
BEGIN
    ALTER TABLE technicians ADD certificate_url NVARCHAR(512) NULL;
END;
GO

IF COL_LENGTH('technicians', 'certificate_issue_date') IS NULL
BEGIN
    ALTER TABLE technicians ADD certificate_issue_date DATE NULL;
END;
GO

IF COL_LENGTH('technicians', 'certificate_expiry_date') IS NULL
BEGIN
    ALTER TABLE technicians ADD certificate_expiry_date DATE NULL;
END;
GO

-- Backfill technician_id and badge_number if PENDING still present
UPDATE t
SET technician_id = CONCAT('TECH-', FORMAT(t.id, '000000')),
    badge_number = CONCAT('BADGE-', FORMAT(t.id, '000000'))
FROM technicians t
WHERE (t.technician_id = 'PENDING' OR t.technician_id IS NULL)
   OR (t.badge_number = 'PENDING' OR t.badge_number IS NULL);
GO

-- Make technician_id and badge_number non-null after backfill
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.technicians')
      AND name = 'technician_id'
      AND is_nullable = 1
)
BEGIN
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_technicians_identifier' AND object_id = OBJECT_ID('dbo.technicians'))
    BEGIN
        DROP INDEX idx_technicians_identifier ON dbo.technicians;
    END;

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_technicians_identifier' AND object_id = OBJECT_ID('dbo.technicians'))
    BEGIN
        DROP INDEX ux_technicians_identifier ON dbo.technicians;
    END;

    ALTER TABLE dbo.technicians ALTER COLUMN technician_id NVARCHAR(64) NOT NULL;
END;

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.technicians')
      AND name = 'badge_number'
      AND is_nullable = 1
)
BEGIN
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_technicians_badge' AND object_id = OBJECT_ID('dbo.technicians'))
    BEGIN
        DROP INDEX idx_technicians_badge ON dbo.technicians;
    END;

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_technicians_badge' AND object_id = OBJECT_ID('dbo.technicians'))
    BEGIN
        DROP INDEX ux_technicians_badge ON dbo.technicians;
    END;

    ALTER TABLE dbo.technicians ALTER COLUMN badge_number NVARCHAR(64) NOT NULL;
END;
GO

-- Unique constraints/indexes
IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.technicians')
      AND name IN ('ux_technicians_badge', 'idx_technicians_badge')
)
BEGIN
    CREATE UNIQUE INDEX ux_technicians_badge ON dbo.technicians(badge_number);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.technicians')
      AND name IN ('ux_technicians_identifier', 'idx_technicians_identifier')
)
BEGIN
    CREATE UNIQUE INDEX ux_technicians_identifier ON dbo.technicians(technician_id);
END;
GO
