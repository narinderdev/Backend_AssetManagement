-- Security events table for dashboard and auditing

IF OBJECT_ID('security_events', 'U') IS NULL
BEGIN
    CREATE TABLE security_events (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        event_type NVARCHAR(64) NOT NULL,
        category NVARCHAR(32) NOT NULL,
        target_type NVARCHAR(32) NOT NULL,
        target_id BIGINT NULL,
        target_name NVARCHAR(255) NULL,
        performed_by_id BIGINT NULL,
        performed_by NVARCHAR(255) NULL,
        result NVARCHAR(16) NOT NULL,
        details NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_security_events_created DEFAULT SYSUTCDATETIME()
    );

    CREATE INDEX idx_security_event_type ON security_events(event_type);
    CREATE INDEX idx_security_event_category ON security_events(category);
    CREATE INDEX idx_security_event_target_type ON security_events(target_type);
    CREATE INDEX idx_security_event_created_at ON security_events(created_at);
END;
