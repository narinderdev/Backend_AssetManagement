-- Add preferred start/end date/time columns
IF OBJECT_ID('service_requests', 'U') IS NOT NULL AND COL_LENGTH('service_requests', 'preferred_start_date') IS NULL
BEGIN
    ALTER TABLE service_requests ADD preferred_start_date DATE NULL;
END;
GO
IF OBJECT_ID('service_requests', 'U') IS NOT NULL AND COL_LENGTH('service_requests', 'preferred_start_time') IS NULL
BEGIN
    ALTER TABLE service_requests ADD preferred_start_time TIME NULL;
END;
GO
IF OBJECT_ID('service_requests', 'U') IS NOT NULL AND COL_LENGTH('service_requests', 'preferred_end_date') IS NULL
BEGIN
    ALTER TABLE service_requests ADD preferred_end_date DATE NULL;
END;
GO
IF OBJECT_ID('service_requests', 'U') IS NOT NULL AND COL_LENGTH('service_requests', 'preferred_end_time') IS NULL
BEGIN
    ALTER TABLE service_requests ADD preferred_end_time TIME NULL;
END;
GO

-- Migrate old preferred_date_time into preferred_start_* if present
IF OBJECT_ID('service_requests', 'U') IS NOT NULL AND COL_LENGTH('service_requests', 'preferred_date_time') IS NOT NULL
BEGIN
    EXEC('
        UPDATE service_requests
        SET preferred_start_date = CAST(preferred_date_time AS DATE),
            preferred_start_time = CAST(preferred_date_time AS TIME)
        WHERE preferred_date_time IS NOT NULL
          AND preferred_start_date IS NULL
          AND preferred_start_time IS NULL
    ');
END;
GO

-- Drop old combined preferred_date_time column
IF OBJECT_ID('service_requests', 'U') IS NOT NULL AND COL_LENGTH('service_requests', 'preferred_date_time') IS NOT NULL
BEGIN
    ALTER TABLE service_requests DROP COLUMN preferred_date_time;
END;
GO

