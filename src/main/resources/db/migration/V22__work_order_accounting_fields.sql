-- Add accounting fields and work order type link to work_orders

IF COL_LENGTH('work_orders', 'work_order_type_id') IS NULL
BEGIN
    ALTER TABLE work_orders
    ADD work_order_type_id BIGINT NULL;
END;

IF COL_LENGTH('work_orders', 'gl_account') IS NULL
BEGIN
    ALTER TABLE work_orders
    ADD gl_account NVARCHAR(255) NULL;
END;

IF COL_LENGTH('work_orders', 'utility_account') IS NULL
BEGIN
    ALTER TABLE work_orders
    ADD utility_account NVARCHAR(255) NULL;
END;

IF COL_LENGTH('work_orders', 'labor_gl_account') IS NULL
BEGIN
    ALTER TABLE work_orders
    ADD labor_gl_account NVARCHAR(255) NULL;
END;

IF COL_LENGTH('work_orders', 'labor_utility_account') IS NULL
BEGIN
    ALTER TABLE work_orders
    ADD labor_utility_account NVARCHAR(255) NULL;
END;

IF COL_LENGTH('work_orders', 'inventory_gl_account') IS NULL
BEGIN
    ALTER TABLE work_orders
    ADD inventory_gl_account NVARCHAR(255) NULL;
END;

IF COL_LENGTH('work_orders', 'inventory_utility_account') IS NULL
BEGIN
    ALTER TABLE work_orders
    ADD inventory_utility_account NVARCHAR(255) NULL;
END;

-- Optional FK if table exists
IF OBJECT_ID('work_order_types', 'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_work_orders_type'
    )
    BEGIN
        ALTER TABLE work_orders
        ADD CONSTRAINT fk_work_orders_type
            FOREIGN KEY (work_order_type_id) REFERENCES work_order_types(id);
    END;
END;
