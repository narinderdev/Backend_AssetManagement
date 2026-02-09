-- Add combined preferred date-time column if it doesn't exist
IF COL_LENGTH('service_requests', 'preferred_date_time') IS NULL
BEGIN
    ALTER TABLE service_requests
    ADD preferred_date_time DATETIME2 NULL;
END;
GO

-- Migrate existing data using dynamic SQL (to avoid parse-time validation)
IF COL_LENGTH('service_requests', 'preferred_date_time') IS NOT NULL
   AND COL_LENGTH('service_requests', 'preferred_date') IS NOT NULL
BEGIN
    EXEC('
        UPDATE service_requests
        SET preferred_date_time = DATEADD(SECOND,
                DATEDIFF(SECOND, ''00:00:00'', ISNULL(preferred_time, ''00:00:00'')),
                CAST(preferred_date AS DATETIME2))
        WHERE preferred_date_time IS NULL AND preferred_date IS NOT NULL
    ');
END;
GO

-- Drop old columns if they exist
IF COL_LENGTH('service_requests', 'preferred_date') IS NOT NULL
BEGIN
    ALTER TABLE service_requests DROP COLUMN preferred_date;
END;
GO

IF COL_LENGTH('service_requests', 'preferred_time') IS NOT NULL
BEGIN
    ALTER TABLE service_requests DROP COLUMN preferred_time;
END;
GO