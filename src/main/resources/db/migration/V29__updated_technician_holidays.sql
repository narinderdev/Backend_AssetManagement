-- Convert technician-specific holidays to global holidays (one row per date)
IF OBJECT_ID('technician_holidays', 'U') IS NOT NULL
BEGIN
    -- Drop FK and technician column if present
    IF EXISTS (
        SELECT 1 FROM sys.foreign_keys
        WHERE name = 'fk_technician_holidays_technician'
          AND parent_object_id = OBJECT_ID('technician_holidays')
    )
    BEGIN
        ALTER TABLE technician_holidays DROP CONSTRAINT fk_technician_holidays_technician;
    END;

    IF EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'uk_technician_holidays_date'
          AND object_id = OBJECT_ID('technician_holidays')
    )
    BEGIN
        DROP INDEX uk_technician_holidays_date ON technician_holidays;
    END;

    IF COL_LENGTH('technician_holidays', 'technician_id') IS NOT NULL
    BEGIN
        ALTER TABLE technician_holidays DROP COLUMN technician_id;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'uk_technician_holidays_date_global'
          AND object_id = OBJECT_ID('technician_holidays')
    )
    BEGIN
        CREATE UNIQUE INDEX uk_technician_holidays_date_global ON technician_holidays(holiday_date);
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'idx_technician_holidays_type'
          AND object_id = OBJECT_ID('technician_holidays')
    )
    BEGIN
        CREATE INDEX idx_technician_holidays_type ON technician_holidays(holiday_type);
    END;
END;
GO
