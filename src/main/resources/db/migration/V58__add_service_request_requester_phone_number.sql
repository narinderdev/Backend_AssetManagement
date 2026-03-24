IF OBJECT_ID('service_requests', 'U') IS NOT NULL AND COL_LENGTH('service_requests', 'requester_phone_number') IS NULL
BEGIN
    ALTER TABLE service_requests
    ADD requester_phone_number NVARCHAR(20) NULL;
END;
GO

