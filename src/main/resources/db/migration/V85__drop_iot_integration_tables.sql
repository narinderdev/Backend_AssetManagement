IF OBJECT_ID('dbo.iot_alert_actions', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.iot_alert_actions;
END;

IF OBJECT_ID('dbo.iot_alerts', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.iot_alerts;
END;

IF OBJECT_ID('dbo.iot_telemetry_logs', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.iot_telemetry_logs;
END;

IF OBJECT_ID('dbo.iot_devices', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.iot_devices;
END;
