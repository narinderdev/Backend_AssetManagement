-- Work order type template with default accounting and cost treatment

IF OBJECT_ID('work_order_types', 'U') IS NULL
BEGIN
    CREATE TABLE work_order_types (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        work_order_type NVARCHAR(128) NOT NULL,
        default_gl_account NVARCHAR(255) NULL,
        default_utility_account NVARCHAR(255) NULL,
        cost_treatment NVARCHAR(16) NOT NULL,
        labor_gl_account NVARCHAR(255) NULL,
        labor_utility_account NVARCHAR(255) NULL,
        inventory_gl_account NVARCHAR(255) NULL,
        inventory_utility_account NVARCHAR(255) NULL,
        active BIT NOT NULL CONSTRAINT df_work_order_types_active DEFAULT 1,
        created_at DATETIME2 NOT NULL CONSTRAINT df_work_order_types_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_work_order_types_updated DEFAULT SYSUTCDATETIME()
    );

    CREATE UNIQUE INDEX uk_work_order_types_type ON work_order_types(work_order_type);
    CREATE INDEX idx_work_order_types_active ON work_order_types(active);
END;
