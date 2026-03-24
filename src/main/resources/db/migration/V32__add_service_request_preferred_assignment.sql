-- Preferred assignment selections for service requests
IF OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL
BEGIN
IF OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL AND COL_LENGTH('dbo.service_requests', 'preferred_technician_id') IS NULL
    BEGIN
        ALTER TABLE dbo.service_requests ADD preferred_technician_id BIGINT NULL;
    END;
IF OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL AND COL_LENGTH('dbo.service_requests', 'preferred_team_id') IS NULL
    BEGIN
        ALTER TABLE dbo.service_requests ADD preferred_team_id BIGINT NULL;
    END;
END;

