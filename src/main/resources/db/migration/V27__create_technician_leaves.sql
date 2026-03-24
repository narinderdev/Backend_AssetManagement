-- Track technician leaves so calendars can reflect unavailability
IF OBJECT_ID('technician_leaves', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.technician_leaves (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        technician_id BIGINT NOT NULL,
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        reason NVARCHAR(512) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_technician_leaves_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_technician_leaves_updated DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF OBJECT_ID('dbo.technician_leaves', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.technicians', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_technician_leaves_technician'
         AND parent_object_id = OBJECT_ID('dbo.technician_leaves')
   )
BEGIN
    ALTER TABLE dbo.technician_leaves
        ADD CONSTRAINT fk_technician_leaves_technician
        FOREIGN KEY (technician_id) REFERENCES dbo.technicians(id)
        ON DELETE CASCADE;
END;
GO

IF OBJECT_ID('dbo.technician_leaves', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_technician_leaves_technician'
         AND object_id = OBJECT_ID('dbo.technician_leaves')
   )
BEGIN
    CREATE INDEX idx_technician_leaves_technician ON dbo.technician_leaves(technician_id);
END;
GO

IF OBJECT_ID('dbo.technician_leaves', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_technician_leaves_date_range'
         AND object_id = OBJECT_ID('dbo.technician_leaves')
   )
BEGIN
    CREATE INDEX idx_technician_leaves_date_range ON dbo.technician_leaves(start_date, end_date);
END;
GO

IF OBJECT_ID('ck_technician_leaves_date_range', 'C') IS NULL
BEGIN
    ALTER TABLE dbo.technician_leaves WITH CHECK ADD CONSTRAINT ck_technician_leaves_date_range
        CHECK (end_date >= start_date);
END;
GO
