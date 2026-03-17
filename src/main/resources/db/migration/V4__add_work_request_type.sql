-- Create work_request_types lookup table
IF OBJECT_ID('dbo.work_request_types', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.work_request_types (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(32) NOT NULL,
        description NVARCHAR(255) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_work_request_types_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_work_request_types_updated_at DEFAULT SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_work_request_types_code' AND object_id = OBJECT_ID('dbo.work_request_types'))
BEGIN
    CREATE UNIQUE INDEX ux_work_request_types_code ON dbo.work_request_types(code);
END;

-- If table was pre-created (e.g. by Hibernate), ensure defaults exist for timestamp columns.
IF COL_LENGTH('dbo.work_request_types', 'created_at') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.default_constraints dc
       INNER JOIN sys.columns c
           ON c.default_object_id = dc.object_id
       WHERE dc.parent_object_id = OBJECT_ID('dbo.work_request_types')
         AND c.name = 'created_at'
   )
BEGIN
    ALTER TABLE dbo.work_request_types
    ADD CONSTRAINT df_work_request_types_created_at DEFAULT SYSUTCDATETIME() FOR created_at;
END;

IF COL_LENGTH('dbo.work_request_types', 'updated_at') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.default_constraints dc
       INNER JOIN sys.columns c
           ON c.default_object_id = dc.object_id
       WHERE dc.parent_object_id = OBJECT_ID('dbo.work_request_types')
         AND c.name = 'updated_at'
   )
BEGIN
    ALTER TABLE dbo.work_request_types
    ADD CONSTRAINT df_work_request_types_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at;
END;

-- Seed default types (idempotent)
DECLARE @seed TABLE(code NVARCHAR(32), description NVARCHAR(255));
INSERT INTO @seed (code, description) VALUES
 (N'C', N'Capital maintenance (major)'),
 (N'E', N'Expense maintenance (minor)');

INSERT INTO dbo.work_request_types (code, description, created_at, updated_at)
SELECT UPPER(LTRIM(RTRIM(s.code))), s.description, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM @seed s
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.work_request_types wrt
    WHERE UPPER(LTRIM(RTRIM(wrt.code))) = UPPER(LTRIM(RTRIM(s.code)))
);

-- Legacy data migration for work_orders table (skip safely when base table is absent).
IF OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.work_orders', 'work_request_type_id') IS NULL
    BEGIN
        ALTER TABLE dbo.work_orders ADD work_request_type_id BIGINT NULL;
    END;

    UPDATE wo
    SET work_request_type_id = wrt.id
    FROM dbo.work_orders wo
    CROSS JOIN (SELECT TOP 1 id FROM dbo.work_request_types WHERE UPPER(code) = 'E') wrt
    WHERE wo.work_request_type_id IS NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_work_orders_work_request_type')
    BEGIN
        ALTER TABLE dbo.work_orders
        ADD CONSTRAINT fk_work_orders_work_request_type FOREIGN KEY (work_request_type_id) REFERENCES dbo.work_request_types(id);
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_work_orders_work_request_type_id' AND object_id = OBJECT_ID('dbo.work_orders'))
    BEGIN
        CREATE INDEX idx_work_orders_work_request_type_id ON dbo.work_orders(work_request_type_id);
    END;
END;
