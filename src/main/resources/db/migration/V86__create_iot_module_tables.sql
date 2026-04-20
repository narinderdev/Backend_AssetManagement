IF OBJECT_ID('dbo.iot_devices', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.iot_devices (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        device_uid NVARCHAR(120) NOT NULL,
        device_name NVARCHAR(160) NOT NULL,
        asset_id BIGINT NULL,
        location NVARCHAR(255) NULL,
        secret_hash NVARCHAR(255) NOT NULL,
        enabled BIT NOT NULL CONSTRAINT df_iot_devices_enabled DEFAULT 1,
        last_seen_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_iot_devices_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END;

IF OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_iot_devices_company_uid' AND object_id = OBJECT_ID('dbo.iot_devices'))
BEGIN
    CREATE UNIQUE INDEX ux_iot_devices_company_uid ON dbo.iot_devices(company_id, device_uid);
END;

IF OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_devices_company' AND object_id = OBJECT_ID('dbo.iot_devices'))
BEGIN
    CREATE INDEX idx_iot_devices_company ON dbo.iot_devices(company_id);
END;

IF OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_devices_asset' AND object_id = OBJECT_ID('dbo.iot_devices'))
BEGIN
    CREATE INDEX idx_iot_devices_asset ON dbo.iot_devices(asset_id);
END;

IF OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.assets', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_devices_asset')
BEGIN
    ALTER TABLE dbo.iot_devices
    ADD CONSTRAINT fk_iot_devices_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets(id);
END;

IF OBJECT_ID('dbo.iot_metric_catalog', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.iot_metric_catalog (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        metric_code NVARCHAR(64) NOT NULL,
        metric_name NVARCHAR(160) NOT NULL,
        unit NVARCHAR(32) NULL,
        description NVARCHAR(500) NULL,
        active BIT NOT NULL CONSTRAINT df_iot_metric_catalog_active DEFAULT 1,
        created_at DATETIME2 NOT NULL CONSTRAINT df_iot_metric_catalog_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END;

IF OBJECT_ID('dbo.iot_metric_catalog', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_iot_metric_catalog_company_code' AND object_id = OBJECT_ID('dbo.iot_metric_catalog'))
BEGIN
    CREATE UNIQUE INDEX ux_iot_metric_catalog_company_code ON dbo.iot_metric_catalog(company_id, metric_code);
END;

IF OBJECT_ID('dbo.iot_metric_catalog', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_metric_catalog_company_active' AND object_id = OBJECT_ID('dbo.iot_metric_catalog'))
BEGIN
    CREATE INDEX idx_iot_metric_catalog_company_active ON dbo.iot_metric_catalog(company_id, active);
END;

IF OBJECT_ID('dbo.iot_alert_rules', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.iot_alert_rules (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        asset_id BIGINT NOT NULL,
        metric_id BIGINT NOT NULL,
        location NVARCHAR(255) NULL,
        rule_operator NVARCHAR(16) NOT NULL,
        low_threshold FLOAT NULL,
        medium_threshold FLOAT NULL,
        high_threshold FLOAT NULL,
        critical_threshold FLOAT NULL,
        cooldown_minutes INT NOT NULL CONSTRAINT df_iot_alert_rules_cooldown DEFAULT 60,
        spike_delta FLOAT NULL,
        consecutive_abnormal_count INT NULL,
        auto_create_service_request BIT NOT NULL CONSTRAINT df_iot_alert_rules_auto_sr DEFAULT 1,
        active BIT NOT NULL CONSTRAINT df_iot_alert_rules_active DEFAULT 1,
        last_triggered_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_iot_alert_rules_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END;

IF OBJECT_ID('dbo.iot_alert_rules', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_iot_alert_rules_company_asset_metric' AND object_id = OBJECT_ID('dbo.iot_alert_rules'))
BEGIN
    CREATE UNIQUE INDEX ux_iot_alert_rules_company_asset_metric ON dbo.iot_alert_rules(company_id, asset_id, metric_id);
END;

IF OBJECT_ID('dbo.iot_alert_rules', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alert_rules_company_active' AND object_id = OBJECT_ID('dbo.iot_alert_rules'))
BEGIN
    CREATE INDEX idx_iot_alert_rules_company_active ON dbo.iot_alert_rules(company_id, active);
END;

IF OBJECT_ID('dbo.iot_alert_rules', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alert_rules_asset' AND object_id = OBJECT_ID('dbo.iot_alert_rules'))
BEGIN
    CREATE INDEX idx_iot_alert_rules_asset ON dbo.iot_alert_rules(asset_id);
END;

IF OBJECT_ID('dbo.iot_alert_rules', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.assets', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alert_rules_asset')
BEGIN
    ALTER TABLE dbo.iot_alert_rules
    ADD CONSTRAINT fk_iot_alert_rules_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets(id);
END;

IF OBJECT_ID('dbo.iot_alert_rules', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.iot_metric_catalog', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alert_rules_metric')
BEGIN
    ALTER TABLE dbo.iot_alert_rules
    ADD CONSTRAINT fk_iot_alert_rules_metric FOREIGN KEY (metric_id) REFERENCES dbo.iot_metric_catalog(id);
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.iot_telemetry_logs (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        device_id BIGINT NULL,
        asset_id BIGINT NULL,
        metric_id BIGINT NULL,
        metric_code NVARCHAR(64) NOT NULL,
        event_id NVARCHAR(128) NULL,
        observed_at DATETIME2 NOT NULL,
        reading_value FLOAT NOT NULL,
        location NVARCHAR(255) NULL,
        ingest_status NVARCHAR(16) NOT NULL,
        alert_severity NVARCHAR(16) NULL,
        anomaly_type NVARCHAR(32) NULL,
        processing_attempts INT NOT NULL CONSTRAINT df_iot_telemetry_attempts DEFAULT 0,
        last_error NVARCHAR(1000) NULL,
        processed_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_iot_telemetry_created_at DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_iot_telemetry_company_device_event' AND object_id = OBJECT_ID('dbo.iot_telemetry_logs'))
BEGIN
    CREATE UNIQUE INDEX ux_iot_telemetry_company_device_event
    ON dbo.iot_telemetry_logs(company_id, device_id, event_id)
    WHERE device_id IS NOT NULL AND event_id IS NOT NULL;
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_telemetry_company_observed' AND object_id = OBJECT_ID('dbo.iot_telemetry_logs'))
BEGIN
    CREATE INDEX idx_iot_telemetry_company_observed ON dbo.iot_telemetry_logs(company_id, observed_at DESC);
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_telemetry_company_status_created' AND object_id = OBJECT_ID('dbo.iot_telemetry_logs'))
BEGIN
    CREATE INDEX idx_iot_telemetry_company_status_created ON dbo.iot_telemetry_logs(company_id, ingest_status, created_at DESC);
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_telemetry_asset_metric_time' AND object_id = OBJECT_ID('dbo.iot_telemetry_logs'))
BEGIN
    CREATE INDEX idx_iot_telemetry_asset_metric_time ON dbo.iot_telemetry_logs(asset_id, metric_code, observed_at DESC);
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
   AND OBJECT_ID('dbo.iot_metric_catalog', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_telemetry_metric')
BEGIN
    ALTER TABLE dbo.iot_telemetry_logs
    ADD CONSTRAINT fk_iot_telemetry_metric FOREIGN KEY (metric_id) REFERENCES dbo.iot_metric_catalog(id);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.iot_alerts (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        device_id BIGINT NULL,
        asset_id BIGINT NULL,
        rule_id BIGINT NULL,
        metric_id BIGINT NULL,
        metric_code NVARCHAR(64) NOT NULL,
        latest_value FLOAT NULL,
        threshold_value FLOAT NULL,
        severity NVARCHAR(16) NOT NULL,
        anomaly_type NVARCHAR(32) NOT NULL,
        status NVARCHAR(20) NOT NULL,
        location NVARCHAR(255) NULL,
        message NVARCHAR(1000) NULL,
        occurred_at DATETIME2 NOT NULL,
        last_triggered_at DATETIME2 NOT NULL,
        last_normal_at DATETIME2 NULL,
        healthy_streak INT NOT NULL CONSTRAINT df_iot_alerts_healthy_streak DEFAULT 0,
        linked_service_request_id BIGINT NULL,
        acknowledged_by NVARCHAR(255) NULL,
        acknowledged_at DATETIME2 NULL,
        resolved_by NVARCHAR(255) NULL,
        resolved_at DATETIME2 NULL,
        suppressed_until DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_iot_alerts_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alerts_company_status_time' AND object_id = OBJECT_ID('dbo.iot_alerts'))
BEGIN
    CREATE INDEX idx_iot_alerts_company_status_time ON dbo.iot_alerts(company_id, status, occurred_at DESC);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alerts_asset_metric_status' AND object_id = OBJECT_ID('dbo.iot_alerts'))
BEGIN
    CREATE INDEX idx_iot_alerts_asset_metric_status ON dbo.iot_alerts(asset_id, metric_code, status, last_triggered_at DESC);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alerts_service_request' AND object_id = OBJECT_ID('dbo.iot_alerts'))
BEGIN
    CREATE INDEX idx_iot_alerts_service_request ON dbo.iot_alerts(linked_service_request_id);
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
   AND OBJECT_ID('dbo.iot_alert_rules', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alerts_rule')
BEGIN
    ALTER TABLE dbo.iot_alerts
    ADD CONSTRAINT fk_iot_alerts_rule FOREIGN KEY (rule_id) REFERENCES dbo.iot_alert_rules(id);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.iot_metric_catalog', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alerts_metric')
BEGIN
    ALTER TABLE dbo.iot_alerts
    ADD CONSTRAINT fk_iot_alerts_metric FOREIGN KEY (metric_id) REFERENCES dbo.iot_metric_catalog(id);
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alerts_service_request')
BEGIN
    ALTER TABLE dbo.iot_alerts
    ADD CONSTRAINT fk_iot_alerts_service_request FOREIGN KEY (linked_service_request_id) REFERENCES dbo.service_requests(id);
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
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alert_actions_alert' AND object_id = OBJECT_ID('dbo.iot_alert_actions'))
BEGIN
    CREATE INDEX idx_iot_alert_actions_alert ON dbo.iot_alert_actions(alert_id, created_at DESC);
END;

IF OBJECT_ID('dbo.iot_alert_actions', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_iot_alert_actions_company' AND object_id = OBJECT_ID('dbo.iot_alert_actions'))
BEGIN
    CREATE INDEX idx_iot_alert_actions_company ON dbo.iot_alert_actions(company_id, created_at DESC);
END;

IF OBJECT_ID('dbo.iot_alert_actions', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_iot_alert_actions_alert')
BEGIN
    ALTER TABLE dbo.iot_alert_actions
    ADD CONSTRAINT fk_iot_alert_actions_alert FOREIGN KEY (alert_id) REFERENCES dbo.iot_alerts(id);
END;

DECLARE @moduleConstraintName sysname;

IF OBJECT_ID('dbo.app_permissions', 'U') IS NOT NULL
BEGIN
    SELECT TOP 1 @moduleConstraintName = dc.name
    FROM sys.check_constraints dc
    WHERE dc.parent_object_id = OBJECT_ID('dbo.app_permissions')
      AND dc.definition LIKE '%module%';

    IF @moduleConstraintName IS NOT NULL
    BEGIN
        DECLARE @dropModuleConstraintSql NVARCHAR(400);
        SET @dropModuleConstraintSql = N'ALTER TABLE dbo.app_permissions DROP CONSTRAINT [' + @moduleConstraintName + N']';
        EXEC sp_executesql @dropModuleConstraintSql;
    END;

    ALTER TABLE dbo.app_permissions WITH CHECK ADD CONSTRAINT CK_app_permissions_module
    CHECK (module IN (
        'ASSET',
        'ASSET_TYPE',
        'SERVICE_REQUEST',
        'WORK_ORDER',
        'WORK_ORDER_TYPE',
        'CORRECTIVE_MAINTENANCE',
        'PREVENTIVE_MAINTENANCE',
        'MATERIAL_REQUISITION',
        'PURCHASE_ORDER',
        'GOODS_RECEIPT_NOTE',
        'VENDOR',
        'INVENTORY',
        'TECHNICIAN',
        'TECHNICIAN_TEAM',
        'DASHBOARD',
        'REPORTS',
        'MANAGE_USERS',
        'MANAGE_ROLES',
        'INVITE_USER',
        'IOT'
    ));

    ALTER TABLE dbo.app_permissions CHECK CONSTRAINT CK_app_permissions_module;

    MERGE dbo.app_permissions AS tgt
    USING (VALUES
        ('create_iot', 'IOT', 'CREATE', 'Create IoT', 'Allows user to create IoT configuration', 200, 1),
        ('view_iot', 'IOT', 'VIEW', 'View IoT', 'Allows user to view IoT data', 201, 1),
        ('update_iot', 'IOT', 'UPDATE', 'Update IoT', 'Allows user to update IoT configuration', 202, 1),
        ('delete_iot', 'IOT', 'DELETE', 'Delete IoT', 'Allows user to delete IoT configuration', 203, 1),
        ('view_iot_dashboard', 'IOT', 'VIEW', 'View IoT Dashboard', 'Allows user to view IoT dashboard', 204, 1),
        ('view_iot_alerts', 'IOT', 'VIEW', 'View IoT Alerts', 'Allows user to view IoT alerts', 205, 1),
        ('update_iot_alerts', 'IOT', 'UPDATE', 'Update IoT Alerts', 'Allows user to acknowledge or resolve IoT alerts', 206, 1)
    ) AS src(code, module, action, label, description, sort_order, active)
    ON tgt.code = src.code
    WHEN MATCHED THEN
        UPDATE SET
            tgt.module = src.module,
            tgt.action = src.action,
            tgt.label = src.label,
            tgt.description = src.description,
            tgt.sort_order = src.sort_order,
            tgt.active = src.active
    WHEN NOT MATCHED THEN
        INSERT (code, module, action, label, description, sort_order, active)
        VALUES (src.code, src.module, src.action, src.label, src.description, src.sort_order, src.active);
END;

IF OBJECT_ID('dbo.permission_objects', 'U') IS NOT NULL
BEGIN
    MERGE dbo.permission_objects AS tgt
    USING (VALUES
        (CAST(29 AS DECIMAL(19,0)), CAST(4 AS DECIMAL(19,0)), 'IoT', 'IOT', CAST(0 AS SMALLINT), CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(30 AS DECIMAL(19,0)), CAST(1 AS DECIMAL(19,0)), 'IoT Dashboard', 'IOT', CAST(1 AS SMALLINT), CAST(4 AS DECIMAL(10,0)), CAST(1 AS SMALLINT))
    ) AS src(id, class_id, name, module, view_only, sort_order, active)
    ON tgt.id = src.id
    WHEN MATCHED THEN
        UPDATE SET
            tgt.class_id = src.class_id,
            tgt.name = src.name,
            tgt.module = src.module,
            tgt.view_only = src.view_only,
            tgt.sort_order = src.sort_order,
            tgt.active = src.active
    WHEN NOT MATCHED THEN
        INSERT (id, class_id, name, module, view_only, sort_order, active)
        VALUES (src.id, src.class_id, src.name, src.module, src.view_only, src.sort_order, src.active);
END;

