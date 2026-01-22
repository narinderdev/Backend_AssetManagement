-- Create work_order_number column for existing work_orders
IF COL_LENGTH('work_orders', 'work_order_number') IS NULL
BEGIN
    ALTER TABLE work_orders ADD work_order_number NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH('work_orders', 'work_order_number') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_work_orders_work_order_number' AND object_id = OBJECT_ID('work_orders'))
BEGIN
    CREATE UNIQUE INDEX ux_work_orders_work_order_number ON work_orders(work_order_number) WHERE work_order_number IS NOT NULL;
END;
GO

-- WO number pool table
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'wo_number_pool')
BEGIN
    CREATE TABLE wo_number_pool (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        wo_number NVARCHAR(50) NOT NULL,
        is_assigned BIT NOT NULL CONSTRAINT df_wo_number_pool_is_assigned DEFAULT 0,
        assigned_to_wo_id BIGINT NULL,
        assigned_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_wo_number_pool_created_at DEFAULT SYSUTCDATETIME()
    );

    CREATE UNIQUE INDEX ux_wo_number_pool_wo_number ON wo_number_pool(wo_number);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_wo_number_pool_wo_number' AND object_id = OBJECT_ID('wo_number_pool'))
BEGIN
    CREATE UNIQUE INDEX ux_wo_number_pool_wo_number ON wo_number_pool(wo_number);
END;

-- Seed first 1000 numbers
;WITH nums AS (
    SELECT TOP (1000) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects a CROSS JOIN sys.all_objects b
)
INSERT INTO wo_number_pool (wo_number)
SELECT CAST(n AS NVARCHAR(50))
FROM nums
WHERE NOT EXISTS (
    SELECT 1 FROM wo_number_pool p WHERE p.wo_number = CAST(n AS NVARCHAR(50))
);

-- Ensure pool has enough numbers to cover existing work orders plus buffer
DECLARE @missing INT = (SELECT COUNT(*) FROM work_orders WHERE work_order_number IS NULL);
DECLARE @available INT = (SELECT COUNT(*) FROM wo_number_pool WHERE is_assigned = 0);
DECLARE @needed INT = (@missing - @available) + 100;

IF @needed > 0
BEGIN
    DECLARE @currentMax BIGINT;
    SELECT @currentMax = ISNULL(MAX(CAST(wo_number AS BIGINT)), 0)
    FROM wo_number_pool WITH (UPDLOCK, HOLDLOCK);

    ;WITH seq AS (
        SELECT TOP (@needed) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn
        FROM sys.all_objects a CROSS JOIN sys.all_objects b
    )
    INSERT INTO wo_number_pool (wo_number)
    SELECT CAST(@currentMax + rn AS NVARCHAR(50))
    FROM seq;
END;

-- Helper procedure to generate more numbers
IF OBJECT_ID('sp_generate_wo_numbers', 'P') IS NOT NULL
    DROP PROCEDURE sp_generate_wo_numbers;
GO
CREATE PROCEDURE sp_generate_wo_numbers @batch_size INT = 100
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @currentMax BIGINT;
    SELECT @currentMax = ISNULL(MAX(CAST(wo_number AS BIGINT)), 0)
    FROM wo_number_pool WITH (UPDLOCK, HOLDLOCK);

    IF @batch_size IS NULL OR @batch_size < 1
        SET @batch_size = 100;

    ;WITH seq AS (
        SELECT TOP (@batch_size) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn
        FROM sys.all_objects a CROSS JOIN sys.all_objects b
    )
    INSERT INTO wo_number_pool (wo_number)
    SELECT CAST(@currentMax + rn AS NVARCHAR(50))
    FROM seq;
END;
GO

-- Backfill existing work orders without a number using available pool
;WITH existing_wo AS (
    SELECT id,
           ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM work_orders
    WHERE work_order_number IS NULL
),
available_pool AS (
    SELECT id,
           wo_number,
           ROW_NUMBER() OVER (ORDER BY CAST(wo_number AS BIGINT)) AS rn
    FROM wo_number_pool
    WHERE is_assigned = 0
)
UPDATE wo
SET work_order_number = p.wo_number
FROM work_orders wo
JOIN existing_wo e ON wo.id = e.id
JOIN available_pool p ON e.rn = p.rn;

-- Mark assigned numbers
UPDATE p
SET is_assigned = 1,
    assigned_to_wo_id = wo.id,
    assigned_at = SYSUTCDATETIME()
FROM wo_number_pool p
JOIN work_orders wo ON wo.work_order_number = p.wo_number
WHERE p.is_assigned = 0;
