-- Track technician-specific holidays
IF OBJECT_ID('technician_holidays', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.technician_holidays (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        technician_id BIGINT NOT NULL,
        holiday_name NVARCHAR(255) NOT NULL,
        holiday_date DATE NOT NULL,
        holiday_type NVARCHAR(32) NOT NULL,
        notes NVARCHAR(512) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_technician_holidays_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_technician_holidays_updated DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF OBJECT_ID('dbo.technician_holidays', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.technicians', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_technician_holidays_technician'
         AND parent_object_id = OBJECT_ID('dbo.technician_holidays')
   )
BEGIN
    ALTER TABLE dbo.technician_holidays
        ADD CONSTRAINT fk_technician_holidays_technician
        FOREIGN KEY (technician_id) REFERENCES dbo.technicians(id)
        ON DELETE CASCADE;
END;
GO

IF OBJECT_ID('dbo.technician_holidays', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'uk_technician_holidays_date'
         AND object_id = OBJECT_ID('dbo.technician_holidays')
   )
BEGIN
    CREATE UNIQUE INDEX uk_technician_holidays_date ON dbo.technician_holidays(technician_id, holiday_date);
END;
GO

IF OBJECT_ID('dbo.technician_holidays', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_technician_holidays_type'
         AND object_id = OBJECT_ID('dbo.technician_holidays')
   )
BEGIN
    CREATE INDEX idx_technician_holidays_type ON dbo.technician_holidays(holiday_type);
END;
GO
