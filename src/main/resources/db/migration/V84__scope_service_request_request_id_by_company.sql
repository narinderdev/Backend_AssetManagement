IF OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL
BEGIN
    DECLARE @dropSql NVARCHAR(MAX) = N'';

    ;WITH single_col_uq_constraints AS (
        SELECT kc.name AS constraint_name
        FROM sys.key_constraints kc
        JOIN sys.tables t ON t.object_id = kc.parent_object_id
        JOIN sys.schemas s ON s.schema_id = t.schema_id
        JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
        JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
        WHERE kc.[type] = 'UQ'
          AND s.name = 'dbo'
          AND t.name = 'service_requests'
        GROUP BY kc.name, kc.parent_object_id
        HAVING COUNT(*) = 1
           AND MAX(c.name) = 'request_id'
    )
    SELECT @dropSql = @dropSql + N'ALTER TABLE dbo.service_requests DROP CONSTRAINT [' + constraint_name + N'];'
    FROM single_col_uq_constraints;

    IF LEN(@dropSql) > 0
    BEGIN
        EXEC sp_executesql @dropSql;
    END;

    SET @dropSql = N'';

    ;WITH single_col_unique_indexes AS (
        SELECT i.name AS index_name
        FROM sys.indexes i
        JOIN sys.tables t ON t.object_id = i.object_id
        JOIN sys.schemas s ON s.schema_id = t.schema_id
        JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
        JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
        WHERE s.name = 'dbo'
          AND t.name = 'service_requests'
          AND i.is_unique = 1
          AND i.is_primary_key = 0
          AND i.is_unique_constraint = 0
        GROUP BY i.name, i.object_id
        HAVING COUNT(*) = 1
           AND MAX(c.name) = 'request_id'
    )
    SELECT @dropSql = @dropSql + N'DROP INDEX [' + index_name + N'] ON dbo.service_requests;'
    FROM single_col_unique_indexes;

    IF LEN(@dropSql) > 0
    BEGIN
        EXEC sp_executesql @dropSql;
    END;
END;

IF OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.service_requests', 'company_id') IS NOT NULL
   AND COL_LENGTH('dbo.service_requests', 'request_id') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'uk_service_requests_company_request_id'
          AND object_id = OBJECT_ID('dbo.service_requests')
   )
BEGIN
    CREATE UNIQUE INDEX uk_service_requests_company_request_id
        ON dbo.service_requests(company_id, request_id);
END;
