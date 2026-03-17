-- Create work_order_number column for existing work_orders
IF OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.work_orders', 'work_order_number') IS NULL
BEGIN
    ALTER TABLE dbo.work_orders ADD work_order_number NVARCHAR(50) NULL;
END;
GO

IF OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.work_orders', 'work_order_number') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_work_orders_work_order_number' AND object_id = OBJECT_ID('dbo.work_orders'))
BEGIN
    CREATE UNIQUE INDEX ux_work_orders_work_order_number ON dbo.work_orders(work_order_number) WHERE work_order_number IS NOT NULL;
END;
GO

-- WO number pool table
IF OBJECT_ID('dbo.wo_number_pool', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.wo_number_pool (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        wo_number NVARCHAR(50) NOT NULL,
        is_assigned BIT NOT NULL CONSTRAINT df_wo_number_pool_is_assigned DEFAULT 0,
        assigned_to_wo_id BIGINT NULL,
        assigned_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_wo_number_pool_created_at DEFAULT SYSUTCDATETIME()
    );

    CREATE UNIQUE INDEX ux_wo_number_pool_wo_number ON dbo.wo_number_pool(wo_number);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_wo_number_pool_wo_number' AND object_id = OBJECT_ID('dbo.wo_number_pool'))
BEGIN
    CREATE UNIQUE INDEX ux_wo_number_pool_wo_number ON dbo.wo_number_pool(wo_number);
END;

-- If table was pre-created (e.g. by Hibernate), ensure defaults exist.
IF COL_LENGTH('dbo.wo_number_pool', 'is_assigned') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.default_constraints dc
       INNER JOIN sys.columns c
           ON c.default_object_id = dc.object_id
       WHERE dc.parent_object_id = OBJECT_ID('dbo.wo_number_pool')
         AND c.name = 'is_assigned'
   )
BEGIN
    ALTER TABLE dbo.wo_number_pool
    ADD CONSTRAINT df_wo_number_pool_is_assigned DEFAULT 0 FOR is_assigned;
END;

IF COL_LENGTH('dbo.wo_number_pool', 'created_at') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.default_constraints dc
       INNER JOIN sys.columns c
           ON c.default_object_id = dc.object_id
       WHERE dc.parent_object_id = OBJECT_ID('dbo.wo_number_pool')
         AND c.name = 'created_at'
   )
BEGIN
    ALTER TABLE dbo.wo_number_pool
    ADD CONSTRAINT df_wo_number_pool_created_at DEFAULT SYSUTCDATETIME() FOR created_at;
END;

-- Seed first 1000 numbers
;WITH nums AS (
    SELECT TOP (1000) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects a CROSS JOIN sys.all_objects b
)
INSERT INTO dbo.wo_number_pool (wo_number, is_assigned, created_at)
SELECT CAST(n AS NVARCHAR(50)), 0, SYSUTCDATETIME()
FROM nums
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.wo_number_pool p WHERE p.wo_number = CAST(n AS NVARCHAR(50))
);

-- Ensure pool has enough numbers to cover existing work orders plus buffer
DECLARE @missing INT = 0;
IF OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
BEGIN
    SELECT @missing = COUNT(*) FROM dbo.work_orders WHERE work_order_number IS NULL;
END;

DECLARE @available INT = (SELECT COUNT(*) FROM dbo.wo_number_pool WHERE is_assigned = 0);
DECLARE @needed INT = (@missing - @available) + 100;

IF @needed > 0
BEGIN
    DECLARE @currentMax BIGINT;
    SELECT @currentMax = ISNULL(MAX(CAST(wo_number AS BIGINT)), 0)
    FROM dbo.wo_number_pool WITH (UPDLOCK, HOLDLOCK);

    ;WITH seq AS (
        SELECT TOP (@needed) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn
        FROM sys.all_objects a CROSS JOIN sys.all_objects b
    )
    INSERT INTO dbo.wo_number_pool (wo_number, is_assigned, created_at)
    SELECT CAST(@currentMax + rn AS NVARCHAR(50)), 0, SYSUTCDATETIME()
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
    FROM dbo.wo_number_pool WITH (UPDLOCK, HOLDLOCK);

    IF @batch_size IS NULL OR @batch_size < 1
        SET @batch_size = 100;

    ;WITH seq AS (
        SELECT TOP (@batch_size) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn
        FROM sys.all_objects a CROSS JOIN sys.all_objects b
    )
    INSERT INTO dbo.wo_number_pool (wo_number, is_assigned, created_at)
    SELECT CAST(@currentMax + rn AS NVARCHAR(50)), 0, SYSUTCDATETIME()
    FROM seq;
END;
GO

-- Backfill existing work orders without a number using available pool
IF OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
BEGIN
    ;WITH existing_wo AS (
        SELECT id,
               ROW_NUMBER() OVER (ORDER BY id) AS rn
        FROM dbo.work_orders
        WHERE work_order_number IS NULL
    ),
    available_pool AS (
        SELECT id,
               wo_number,
               ROW_NUMBER() OVER (ORDER BY CAST(wo_number AS BIGINT)) AS rn
        FROM dbo.wo_number_pool
        WHERE is_assigned = 0
    )
    UPDATE wo
    SET work_order_number = p.wo_number
    FROM dbo.work_orders wo
    JOIN existing_wo e ON wo.id = e.id
    JOIN available_pool p ON e.rn = p.rn;

    -- Mark assigned numbers
    UPDATE p
    SET is_assigned = 1,
        assigned_to_wo_id = wo.id,
        assigned_at = SYSUTCDATETIME()
    FROM dbo.wo_number_pool p
    JOIN dbo.work_orders wo ON wo.work_order_number = p.wo_number
    WHERE p.is_assigned = 0;
END;
