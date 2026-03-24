-- Add company_id to major domain tables for company-wise segregation.
-- Kept nullable to avoid breaking existing data.

IF OBJECT_ID('dbo.assets', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('assets', 'company_id') IS NULL
        ALTER TABLE dbo.assets ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('work_orders', 'company_id') IS NULL
        ALTER TABLE dbo.work_orders ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('service_requests', 'company_id') IS NULL
        ALTER TABLE dbo.service_requests ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('vendors', 'company_id') IS NULL
        ALTER TABLE dbo.vendors ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.warehouses', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('warehouses', 'company_id') IS NULL
        ALTER TABLE dbo.warehouses ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('inventory_items', 'company_id') IS NULL
        ALTER TABLE dbo.inventory_items ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.inventory_reconciliations', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('inventory_reconciliations', 'company_id') IS NULL
        ALTER TABLE dbo.inventory_reconciliations ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.material_requisitions', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('material_requisitions', 'company_id') IS NULL
        ALTER TABLE dbo.material_requisitions ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.purchase_orders', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('purchase_orders', 'company_id') IS NULL
        ALTER TABLE dbo.purchase_orders ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.goods_receipt_notes', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('goods_receipt_notes', 'company_id') IS NULL
        ALTER TABLE dbo.goods_receipt_notes ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.purchase_requisitions', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('purchase_requisitions', 'company_id') IS NULL
        ALTER TABLE dbo.purchase_requisitions ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.vendor_returns', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('vendor_returns', 'company_id') IS NULL
        ALTER TABLE dbo.vendor_returns ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.emergency_incidents', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('emergency_incidents', 'company_id') IS NULL
        ALTER TABLE dbo.emergency_incidents ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.pm_plans', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('pm_plans', 'company_id') IS NULL
        ALTER TABLE dbo.pm_plans ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.technicians', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('technicians', 'company_id') IS NULL
        ALTER TABLE dbo.technicians ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.technician_teams', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('technician_teams', 'company_id') IS NULL
        ALTER TABLE dbo.technician_teams ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.work_request_types', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('work_request_types', 'company_id') IS NULL
        ALTER TABLE dbo.work_request_types ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.work_order_types', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('work_order_types', 'company_id') IS NULL
        ALTER TABLE dbo.work_order_types ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.asset_types', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('asset_types', 'company_id') IS NULL
        ALTER TABLE dbo.asset_types ADD company_id BIGINT NULL;
END;

IF OBJECT_ID('dbo.assets', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_assets_company_id' AND object_id = OBJECT_ID('dbo.assets'))
BEGIN
    CREATE INDEX idx_assets_company_id ON dbo.assets(company_id);
END;

IF OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_work_orders_company_id' AND object_id = OBJECT_ID('dbo.work_orders'))
BEGIN
    CREATE INDEX idx_work_orders_company_id ON dbo.work_orders(company_id);
END;

IF OBJECT_ID('dbo.service_requests', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_service_requests_company_id' AND object_id = OBJECT_ID('dbo.service_requests'))
BEGIN
    CREATE INDEX idx_service_requests_company_id ON dbo.service_requests(company_id);
END;

IF OBJECT_ID('dbo.inventory_items', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_inventory_items_company_id' AND object_id = OBJECT_ID('dbo.inventory_items'))
BEGIN
    CREATE INDEX idx_inventory_items_company_id ON dbo.inventory_items(company_id);
END;

IF OBJECT_ID('dbo.vendors', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_vendors_company_id' AND object_id = OBJECT_ID('dbo.vendors'))
BEGIN
    CREATE INDEX idx_vendors_company_id ON dbo.vendors(company_id);
END;

IF OBJECT_ID('dbo.companies', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.assets', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_assets_company_id' AND parent_object_id = OBJECT_ID('dbo.assets'))
BEGIN
    ALTER TABLE dbo.assets
        ADD CONSTRAINT fk_assets_company_id FOREIGN KEY (company_id) REFERENCES dbo.companies(id);
END;

IF OBJECT_ID('dbo.companies', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.work_orders', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_work_orders_company_id' AND parent_object_id = OBJECT_ID('dbo.work_orders'))
BEGIN
    ALTER TABLE dbo.work_orders
        ADD CONSTRAINT fk_work_orders_company_id FOREIGN KEY (company_id) REFERENCES dbo.companies(id);
END;
