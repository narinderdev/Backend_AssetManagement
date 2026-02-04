-- Track technician-specific holidays
IF OBJECT_ID('technician_holidays', 'U') IS NULL
BEGIN
    CREATE TABLE technician_holidays (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        technician_id BIGINT NOT NULL,
        holiday_name NVARCHAR(255) NOT NULL,
        holiday_date DATE NOT NULL,
        holiday_type NVARCHAR(32) NOT NULL,
        notes NVARCHAR(512) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_technician_holidays_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_technician_holidays_updated DEFAULT SYSUTCDATETIME()
    );

    ALTER TABLE technician_holidays
        ADD CONSTRAINT fk_technician_holidays_technician
        FOREIGN KEY (technician_id) REFERENCES technicians(id)
        ON DELETE CASCADE;

    CREATE UNIQUE INDEX uk_technician_holidays_date ON technician_holidays(technician_id, holiday_date);
    CREATE INDEX idx_technician_holidays_type ON technician_holidays(holiday_type);
END;
GO
