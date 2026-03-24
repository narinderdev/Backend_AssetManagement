-- Scope work_order_types uniqueness and lookup by company.

IF OBJECT_ID('dbo.work_order_types', 'U') IS NOT NULL
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE name = 'uk_work_order_types_type'
          AND parent_object_id = OBJECT_ID('dbo.work_order_types')
    )
    BEGIN
        ALTER TABLE dbo.work_order_types DROP CONSTRAINT uk_work_order_types_type;
    END;

    IF EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'uk_work_order_types_type'
          AND object_id = OBJECT_ID('dbo.work_order_types')
    )
    BEGIN
        DROP INDEX uk_work_order_types_type ON dbo.work_order_types;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'uk_work_order_types_company_type'
          AND object_id = OBJECT_ID('dbo.work_order_types')
    )
    BEGIN
        CREATE UNIQUE INDEX uk_work_order_types_company_type
            ON dbo.work_order_types(company_id, work_order_type);
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'idx_work_order_types_company_id'
          AND object_id = OBJECT_ID('dbo.work_order_types')
    )
    BEGIN
        CREATE INDEX idx_work_order_types_company_id ON dbo.work_order_types(company_id);
    END;
END;

IF OBJECT_ID('dbo.companies', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.work_order_types', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_work_order_types_company_id'
         AND parent_object_id = OBJECT_ID('dbo.work_order_types')
   )
BEGIN
    ALTER TABLE dbo.work_order_types
        ADD CONSTRAINT fk_work_order_types_company_id
            FOREIGN KEY (company_id) REFERENCES dbo.companies(id);
END;
