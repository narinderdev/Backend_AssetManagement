IF OBJECT_ID('dbo.iot_devices', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.iot_devices (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        device_uid NVARCHAR(120) NOT NULL,
        device_name NVARCHAR(160) NOT NULL,
        asset_id BIGINT NULL,
        location NVARCHAR(255) NULL,
        auth_token_hash NVARCHAR(255) NOT NULL,
        enabled BIT NOT NULL CONSTRAINT df_iot_devices_enabled DEFAULT 1,
        last_seen_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_iot_devices_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END;

IF OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_iot_device_company_uid' AND object_id = OBJECT_ID('dbo.iot_devices'))
BEGIN
    CREATE UNIQUE INDEX ux_iot_device_company_uid ON dbo.iot_devices(company_id, device_uid);
END;

IF OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_device_company' AND object_id = OBJECT_ID('dbo.iot_devices'))
BEGIN
    CREATE INDEX idx_iot_device_company ON dbo.iot_devices(company_id);
END;

IF OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_device_asset' AND object_id = OBJECT_ID('dbo.iot_devices'))
BEGIN
    CREATE INDEX idx_iot_device_asset ON dbo.iot_devices(asset_id);
END;

IF OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.assets', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_devices_asset')
BEGIN
    ALTER TABLE dbo.iot_devices
    ADD CONSTRAINT fk_iot_devices_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets(id);
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.iot_telemetry_logs (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        device_id BIGINT NOT NULL,
        asset_id BIGINT NULL,
        meter_type NVARCHAR(32) NOT NULL,
        reading_value FLOAT NOT NULL,
        reading_time DATETIME2 NOT NULL,
        severity NVARCHAR(16) NULL,
        anomaly_type NVARCHAR(32) NULL,
        notes NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_iot_telemetry_created_at DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_telemetry_device')
BEGIN
    ALTER TABLE dbo.iot_telemetry_logs
    ADD CONSTRAINT fk_iot_telemetry_device FOREIGN KEY (device_id) REFERENCES dbo.iot_devices(id);
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.assets', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_telemetry_asset')
BEGIN
    ALTER TABLE dbo.iot_telemetry_logs
    ADD CONSTRAINT fk_iot_telemetry_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets(id);
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_telemetry_company_time' AND object_id = OBJECT_ID('dbo.iot_telemetry_logs'))
BEGIN
    CREATE INDEX idx_iot_telemetry_company_time ON dbo.iot_telemetry_logs(company_id, reading_time DESC);
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_telemetry_device_meter' AND object_id = OBJECT_ID('dbo.iot_telemetry_logs'))
BEGIN
    CREATE INDEX idx_iot_telemetry_device_meter ON dbo.iot_telemetry_logs(device_id, meter_type, reading_time DESC);
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_telemetry_asset' AND object_id = OBJECT_ID('dbo.iot_telemetry_logs'))
BEGIN
    CREATE INDEX idx_iot_telemetry_asset ON dbo.iot_telemetry_logs(asset_id);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.iot_alerts (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        device_id BIGINT NOT NULL,
        asset_id BIGINT NULL,
        meter_type NVARCHAR(32) NOT NULL,
        reading_value FLOAT NOT NULL,
        threshold_value FLOAT NULL,
        severity NVARCHAR(16) NOT NULL,
        alert_type NVARCHAR(32) NOT NULL,
        status NVARCHAR(16) NOT NULL,
        location NVARCHAR(255) NULL,
        message NVARCHAR(1000) NULL,
        occurred_at DATETIME2 NOT NULL,
        service_request_id BIGINT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_iot_alerts_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alerts_device')
BEGIN
    ALTER TABLE dbo.iot_alerts
    ADD CONSTRAINT fk_iot_alerts_device FOREIGN KEY (device_id) REFERENCES dbo.iot_devices(id);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.assets', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alerts_asset')
BEGIN
    ALTER TABLE dbo.iot_alerts
    ADD CONSTRAINT fk_iot_alerts_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets(id);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alerts_service_request')
BEGIN
    ALTER TABLE dbo.iot_alerts
    ADD CONSTRAINT fk_iot_alerts_service_request FOREIGN KEY (service_request_id) REFERENCES dbo.service_requests(id);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alert_company_status' AND object_id = OBJECT_ID('dbo.iot_alerts'))
BEGIN
    CREATE INDEX idx_iot_alert_company_status ON dbo.iot_alerts(company_id, status, occurred_at DESC);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alert_device_meter' AND object_id = OBJECT_ID('dbo.iot_alerts'))
BEGIN
    CREATE INDEX idx_iot_alert_device_meter ON dbo.iot_alerts(device_id, meter_type, occurred_at DESC);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alert_asset' AND object_id = OBJECT_ID('dbo.iot_alerts'))
BEGIN
    CREATE INDEX idx_iot_alert_asset ON dbo.iot_alerts(asset_id);
END;

IF OBJECT_ID('dbo.iot_alert_actions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.iot_alert_actions (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        alert_id BIGINT NOT NULL,
        company_id BIGINT NOT NULL,
        action_type NVARCHAR(64) NOT NULL,
        action_by NVARCHAR(255) NULL,
        action_details NVARCHAR(1000) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_iot_alert_actions_created_at DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID('dbo.iot_alert_actions', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alert_actions_alert')
BEGIN
    ALTER TABLE dbo.iot_alert_actions
    ADD CONSTRAINT fk_iot_alert_actions_alert FOREIGN KEY (alert_id) REFERENCES dbo.iot_alerts(id);
END;

IF OBJECT_ID('dbo.iot_alert_actions', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alert_action_alert' AND object_id = OBJECT_ID('dbo.iot_alert_actions'))
BEGIN
    CREATE INDEX idx_iot_alert_action_alert ON dbo.iot_alert_actions(alert_id, created_at DESC);
END;

IF OBJECT_ID('dbo.iot_alert_actions', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alert_action_company' AND object_id = OBJECT_ID('dbo.iot_alert_actions'))
BEGIN
    CREATE INDEX idx_iot_alert_action_company ON dbo.iot_alert_actions(company_id, created_at DESC);
END;
