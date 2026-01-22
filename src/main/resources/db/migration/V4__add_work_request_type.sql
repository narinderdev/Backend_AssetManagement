-- Create work_request_types lookup table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'work_request_types')
BEGIN
    CREATE TABLE work_request_types (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(32) NOT NULL,
        description NVARCHAR(255) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_work_request_types_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_work_request_types_updated_at DEFAULT SYSUTCDATETIME()
    );

    CREATE UNIQUE INDEX ux_work_request_types_code ON work_request_types(code);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_work_request_types_code' AND object_id = OBJECT_ID('work_request_types'))
BEGIN
    CREATE UNIQUE INDEX ux_work_request_types_code ON work_request_types(code);
END;

-- Seed default types (idempotent)
DECLARE @seed TABLE(code NVARCHAR(32), description NVARCHAR(255));
INSERT INTO @seed (code, description) VALUES
 (N'C', N'Capital maintenance (major)'),
 (N'E', N'Expense maintenance (minor)');

INSERT INTO work_request_types (code, description)
SELECT UPPER(LTRIM(RTRIM(s.code))), s.description
FROM @seed s
WHERE NOT EXISTS (
    SELECT 1 FROM work_request_types wrt
    WHERE UPPER(LTRIM(RTRIM(wrt.code))) = UPPER(LTRIM(RTRIM(s.code)))
);

-- Add FK column on work_orders if missing
IF COL_LENGTH('work_orders', 'work_request_type_id') IS NULL
BEGIN
    ALTER TABLE work_orders ADD work_request_type_id BIGINT NULL;
END;

-- Backfill existing work orders to default Expense type if null
UPDATE wo
SET work_request_type_id = wrt.id
FROM work_orders wo
CROSS JOIN (SELECT TOP 1 id FROM work_request_types WHERE UPPER(code) = 'E') wrt
WHERE wo.work_request_type_id IS NULL;

-- Foreign key & index
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_work_orders_work_request_type')
BEGIN
    ALTER TABLE work_orders
    ADD CONSTRAINT fk_work_orders_work_request_type FOREIGN KEY (work_request_type_id) REFERENCES work_request_types(id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_work_orders_work_request_type_id' AND object_id = OBJECT_ID('work_orders'))
BEGIN
    CREATE INDEX idx_work_orders_work_request_type_id ON work_orders(work_request_type_id);
END;
