-- Scope asset_types uniqueness and lookup by company.

IF OBJECT_ID('dbo.asset_types', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('asset_types', 'company_id') IS NULL
    BEGIN
        ALTER TABLE dbo.asset_types ADD company_id BIGINT NULL;
    END;

    -- Drop legacy unique constraints/indexes on single-column code/name.
    DECLARE @dropConstraintSql NVARCHAR(MAX) = N'';
    SELECT @dropConstraintSql = @dropConstraintSql + N'ALTER TABLE dbo.asset_types DROP CONSTRAINT [' + kc.name + N'];'
    FROM sys.key_constraints kc
    JOIN sys.indexes i
      ON i.object_id = kc.parent_object_id
     AND i.index_id = kc.unique_index_id
    JOIN sys.index_columns ic
      ON ic.object_id = i.object_id
     AND ic.index_id = i.index_id
    JOIN sys.columns c
      ON c.object_id = ic.object_id
     AND c.column_id = ic.column_id
    WHERE kc.parent_object_id = OBJECT_ID('dbo.asset_types')
      AND kc.type = 'UQ'
    GROUP BY kc.name
    HAVING COUNT(*) = 1 AND MAX(c.name) IN ('code', 'name');

    IF LEN(@dropConstraintSql) > 0
    BEGIN
        EXEC sp_executesql @dropConstraintSql;
    END;

    DECLARE @dropIndexSql NVARCHAR(MAX) = N'';
    SELECT @dropIndexSql = @dropIndexSql + N'DROP INDEX [' + i.name + N'] ON dbo.asset_types;'
    FROM sys.indexes i
    JOIN sys.index_columns ic
      ON ic.object_id = i.object_id
     AND ic.index_id = i.index_id
    JOIN sys.columns c
      ON c.object_id = ic.object_id
     AND c.column_id = ic.column_id
    WHERE i.object_id = OBJECT_ID('dbo.asset_types')
      AND i.is_unique = 1
      AND i.is_primary_key = 0
      AND i.is_unique_constraint = 0
    GROUP BY i.name
    HAVING COUNT(*) = 1 AND MAX(c.name) IN ('code', 'name');

    IF LEN(@dropIndexSql) > 0
    BEGIN
        EXEC sp_executesql @dropIndexSql;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'uk_asset_types_company_code'
          AND object_id = OBJECT_ID('dbo.asset_types')
    )
    BEGIN
        CREATE UNIQUE INDEX uk_asset_types_company_code
            ON dbo.asset_types(company_id, code);
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'uk_asset_types_company_name'
          AND object_id = OBJECT_ID('dbo.asset_types')
    )
    BEGIN
        CREATE UNIQUE INDEX uk_asset_types_company_name
            ON dbo.asset_types(company_id, name);
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'idx_asset_types_company_id'
          AND object_id = OBJECT_ID('dbo.asset_types')
    )
    BEGIN
        CREATE INDEX idx_asset_types_company_id ON dbo.asset_types(company_id);
    END;
END;

IF OBJECT_ID('dbo.companies', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.asset_types', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_asset_types_company_id'
         AND parent_object_id = OBJECT_ID('dbo.asset_types')
   )
BEGIN
    ALTER TABLE dbo.asset_types
        ADD CONSTRAINT fk_asset_types_company_id
            FOREIGN KEY (company_id) REFERENCES dbo.companies(id);
END;
