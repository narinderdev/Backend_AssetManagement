-- Create app_permissions table if it does not exist (SQL Server safe)
IF OBJECT_ID('dbo.app_permissions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.app_permissions (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(80) NOT NULL,
        module NVARCHAR(64) NOT NULL,
        action NVARCHAR(32) NOT NULL,
        label NVARCHAR(150) NOT NULL,
        description NVARCHAR(500) NULL,
        active BIT NOT NULL CONSTRAINT DF_app_permissions_active DEFAULT 1,
        sort_order INT NULL,
        CONSTRAINT uk_permission_code UNIQUE (code)
    );

    CREATE INDEX idx_permission_module ON dbo.app_permissions(module);
    CREATE INDEX idx_permission_code ON dbo.app_permissions(code);
END;

