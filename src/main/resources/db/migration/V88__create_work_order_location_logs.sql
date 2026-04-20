-- Geofence/audit table for technician location updates on work orders
IF OBJECT_ID('dbo.work_order_location_logs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.work_order_location_logs (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NULL,
        work_order_id BIGINT NOT NULL,
        technician_id BIGINT NULL,
        team_id BIGINT NULL,
        latitude DECIMAL(10,7) NOT NULL,
        longitude DECIMAL(10,7) NOT NULL,
        observed_at DATETIME2 NOT NULL,
        distance_meters DECIMAL(10,2) NULL,
        geofence_radius_meters INT NOT NULL CONSTRAINT df_wo_location_logs_radius DEFAULT 100,
        inside_geofence BIT NOT NULL CONSTRAINT df_wo_location_logs_inside DEFAULT 0,
        event_type NVARCHAR(64) NOT NULL,
        status_before NVARCHAR(32) NULL,
        status_after NVARCHAR(32) NULL,
        auto_check_in BIT NOT NULL CONSTRAINT df_wo_location_logs_auto_check_in DEFAULT 0,
        auto_check_out BIT NOT NULL CONSTRAINT df_wo_location_logs_auto_check_out DEFAULT 0,
        created_at DATETIME2 NOT NULL CONSTRAINT df_wo_location_logs_created_at DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID('dbo.work_order_location_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_wo_location_logs_work_order')
BEGIN
    ALTER TABLE dbo.work_order_location_logs
        ADD CONSTRAINT fk_wo_location_logs_work_order
        FOREIGN KEY (work_order_id) REFERENCES dbo.work_orders(id);
END;

IF OBJECT_ID('dbo.work_order_location_logs', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.technicians', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_wo_location_logs_technician')
BEGIN
    ALTER TABLE dbo.work_order_location_logs
        ADD CONSTRAINT fk_wo_location_logs_technician
        FOREIGN KEY (technician_id) REFERENCES dbo.technicians(id);
END;

IF OBJECT_ID('dbo.work_order_location_logs', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.technician_teams', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_wo_location_logs_team')
BEGIN
    ALTER TABLE dbo.work_order_location_logs
        ADD CONSTRAINT fk_wo_location_logs_team
        FOREIGN KEY (team_id) REFERENCES dbo.technician_teams(id);
END;

IF OBJECT_ID('dbo.work_order_location_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_wo_loc_logs_work_order_observed' AND object_id = OBJECT_ID('dbo.work_order_location_logs'))
BEGIN
    CREATE INDEX idx_wo_loc_logs_work_order_observed ON dbo.work_order_location_logs(work_order_id, observed_at);
END;

IF OBJECT_ID('dbo.work_order_location_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_wo_loc_logs_technician_observed' AND object_id = OBJECT_ID('dbo.work_order_location_logs'))
BEGIN
    CREATE INDEX idx_wo_loc_logs_technician_observed ON dbo.work_order_location_logs(technician_id, observed_at);
END;

IF OBJECT_ID('dbo.work_order_location_logs', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_wo_loc_logs_observed' AND object_id = OBJECT_ID('dbo.work_order_location_logs'))
BEGIN
    CREATE INDEX idx_wo_loc_logs_observed ON dbo.work_order_location_logs(observed_at);
END;
