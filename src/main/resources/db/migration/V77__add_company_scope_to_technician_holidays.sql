-- Add company scoping to technician holidays.

IF OBJECT_ID('dbo.technician_holidays', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.technician_holidays', 'company_id') IS NULL
BEGIN
    ALTER TABLE dbo.technician_holidays ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.technician_holidays', 'U') IS NOT NULL
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE name = 'uk_technician_holidays_date'
          AND parent_object_id = OBJECT_ID('dbo.technician_holidays')
    )
    BEGIN
        ALTER TABLE dbo.technician_holidays DROP CONSTRAINT uk_technician_holidays_date;
    END;

    IF EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'uk_technician_holidays_date'
          AND object_id = OBJECT_ID('dbo.technician_holidays')
    )
    BEGIN
        DROP INDEX uk_technician_holidays_date ON dbo.technician_holidays;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'uk_technician_holidays_company_date'
          AND object_id = OBJECT_ID('dbo.technician_holidays')
    )
    BEGIN
        CREATE UNIQUE INDEX uk_technician_holidays_company_date
            ON dbo.technician_holidays(company_id, holiday_date);
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'idx_technician_holidays_company'
          AND object_id = OBJECT_ID('dbo.technician_holidays')
    )
    BEGIN
        CREATE INDEX idx_technician_holidays_company
            ON dbo.technician_holidays(company_id);
    END;
END;

IF OBJECT_ID('dbo.companies', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.technician_holidays', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_technician_holidays_company_id'
         AND parent_object_id = OBJECT_ID('dbo.technician_holidays')
   )
BEGIN
    ALTER TABLE dbo.technician_holidays
        ADD CONSTRAINT fk_technician_holidays_company_id
            FOREIGN KEY (company_id) REFERENCES dbo.companies(id);
END;
