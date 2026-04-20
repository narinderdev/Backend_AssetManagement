IF OBJECT_ID('dbo.voice_ai_intakes', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.voice_ai_intakes (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        external_call_id NVARCHAR(128) NULL,
        intent NVARCHAR(64) NOT NULL,
        outcome NVARCHAR(64) NOT NULL,
        requester_name NVARCHAR(255) NULL,
        requester_phone_number NVARCHAR(32) NULL,
        requester_contact NVARCHAR(255) NULL,
        department NVARCHAR(255) NULL,
        issue_description NVARCHAR(2000) NULL,
        short_title NVARCHAR(255) NULL,
        asset_db_id BIGINT NULL,
        asset_external_id NVARCHAR(128) NULL,
        asset_name NVARCHAR(255) NULL,
        location NVARCHAR(255) NULL,
        request_priority NVARCHAR(32) NULL,
        maintenance_type NVARCHAR(32) NULL,
        transcript NVARCHAR(MAX) NULL,
        structured_summary NVARCHAR(MAX) NULL,
        skill_tags NVARCHAR(MAX) NULL,
        requires_escalation BIT NOT NULL CONSTRAINT df_voice_ai_intakes_requires_escalation DEFAULT 0,
        escalation_reason NVARCHAR(1000) NULL,
        assigned_technician_id BIGINT NULL,
        assigned_technician_name NVARCHAR(255) NULL,
        expected_response_at DATETIME2 NULL,
        customer_confirmation_message NVARCHAR(1000) NULL,
        status_snapshot NVARCHAR(MAX) NULL,
        service_request_id BIGINT NULL,
        work_order_id BIGINT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_voice_ai_intakes_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_voice_ai_intakes_updated_at DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID('dbo.voice_ai_intakes', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_voice_ai_intakes_service_request')
BEGIN
    ALTER TABLE dbo.voice_ai_intakes
        ADD CONSTRAINT fk_voice_ai_intakes_service_request
        FOREIGN KEY (service_request_id) REFERENCES dbo.service_requests(id);
END;

IF OBJECT_ID('dbo.voice_ai_intakes', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_voice_ai_intakes_work_order')
BEGIN
    ALTER TABLE dbo.voice_ai_intakes
        ADD CONSTRAINT fk_voice_ai_intakes_work_order
        FOREIGN KEY (work_order_id) REFERENCES dbo.work_orders(id);
END;

IF OBJECT_ID('dbo.voice_ai_intakes', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_voice_ai_intakes_company_created' AND object_id = OBJECT_ID('dbo.voice_ai_intakes'))
BEGIN
    CREATE INDEX idx_voice_ai_intakes_company_created ON dbo.voice_ai_intakes(company_id, created_at);
END;

IF OBJECT_ID('dbo.voice_ai_intakes', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_voice_ai_intakes_service_request' AND object_id = OBJECT_ID('dbo.voice_ai_intakes'))
BEGIN
    CREATE INDEX idx_voice_ai_intakes_service_request ON dbo.voice_ai_intakes(service_request_id);
END;

IF OBJECT_ID('dbo.voice_ai_intakes', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_voice_ai_intakes_work_order' AND object_id = OBJECT_ID('dbo.voice_ai_intakes'))
BEGIN
    CREATE INDEX idx_voice_ai_intakes_work_order ON dbo.voice_ai_intakes(work_order_id);
END;
