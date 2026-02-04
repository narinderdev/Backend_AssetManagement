-- Track technician leaves so calendars can reflect unavailability
IF OBJECT_ID('technician_leaves', 'U') IS NULL
BEGIN
    CREATE TABLE technician_leaves (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        technician_id BIGINT NOT NULL,
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        reason NVARCHAR(512) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_technician_leaves_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_technician_leaves_updated DEFAULT SYSUTCDATETIME()
    );

    ALTER TABLE technician_leaves
        ADD CONSTRAINT fk_technician_leaves_technician
        FOREIGN KEY (technician_id) REFERENCES technicians(id)
        ON DELETE CASCADE;

    CREATE INDEX idx_technician_leaves_technician ON technician_leaves(technician_id);
    CREATE INDEX idx_technician_leaves_date_range ON technician_leaves(start_date, end_date);
END;
GO

IF OBJECT_ID('ck_technician_leaves_date_range', 'C') IS NULL
BEGIN
    ALTER TABLE technician_leaves WITH CHECK ADD CONSTRAINT ck_technician_leaves_date_range
        CHECK (end_date >= start_date);
END;
GO
