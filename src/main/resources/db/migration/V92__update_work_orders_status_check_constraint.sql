-- Align work_orders.status check constraint with current WorkOrderStatus enum values.
IF OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
BEGIN
    DECLARE @dropSql NVARCHAR(MAX) = N'';

    SELECT @dropSql = @dropSql
        + N'ALTER TABLE dbo.work_orders DROP CONSTRAINT [' + cc.name + N'];'
    FROM sys.check_constraints cc
    INNER JOIN sys.columns c
        ON c.object_id = cc.parent_object_id
       AND c.column_id = cc.parent_column_id
    WHERE cc.parent_object_id = OBJECT_ID('dbo.work_orders')
      AND c.name = 'status';

    IF @dropSql <> N''
        EXEC sp_executesql @dropSql;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE name = 'ck_work_orders_status'
          AND parent_object_id = OBJECT_ID('dbo.work_orders')
    )
    BEGIN
        ALTER TABLE dbo.work_orders
            ADD CONSTRAINT ck_work_orders_status CHECK (
                status IN (
                    'NEW',
                    'APPROVED',
                    'REJECTED',
                    'SCHEDULED',
                    'ON_THE_WAY',
                    'ARRIVED',
                    'IN_PROGRESS',
                    'COMPLETED',
                    'CLOSED'
                )
            );
    END
END
