-- V63 may already be marked as applied in some databases due earlier script history.
-- This migration safely ensures the catalog tables and data exist.

IF OBJECT_ID('dbo.permission_classes', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.permission_classes (
        id DECIMAL(19,0) NOT NULL PRIMARY KEY,
        name VARCHAR(120) NOT NULL,
        sort_order DECIMAL(10,0) NOT NULL,
        active SMALLINT NOT NULL CONSTRAINT DF_permission_classes_active DEFAULT 1,
        CONSTRAINT uk_permission_classes_name UNIQUE (name)
    );
END;

IF OBJECT_ID('dbo.permission_objects', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.permission_objects (
        id DECIMAL(19,0) NOT NULL PRIMARY KEY,
        class_id DECIMAL(19,0) NOT NULL,
        name VARCHAR(160) NOT NULL,
        module VARCHAR(64) NOT NULL,
        view_only SMALLINT NOT NULL CONSTRAINT DF_permission_objects_view_only DEFAULT 0,
        sort_order DECIMAL(10,0) NOT NULL,
        active SMALLINT NOT NULL CONSTRAINT DF_permission_objects_active DEFAULT 1,
        CONSTRAINT fk_permission_objects_class FOREIGN KEY (class_id) REFERENCES dbo.permission_classes(id) ON DELETE CASCADE,
        CONSTRAINT uk_permission_objects_class_name UNIQUE (class_id, name)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.permission_objects') AND name = 'idx_permission_objects_class_sort')
BEGIN
    CREATE INDEX idx_permission_objects_class_sort ON dbo.permission_objects(class_id, sort_order);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.permission_objects') AND name = 'idx_permission_objects_module')
BEGIN
    CREATE INDEX idx_permission_objects_module ON dbo.permission_objects(module);
END;

MERGE dbo.permission_classes AS tgt
USING (VALUES
    (CAST(1 AS DECIMAL(19,0)), 'Dashboard', CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(2 AS DECIMAL(19,0)), 'Asset', CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(3 AS DECIMAL(19,0)), 'Work Order', CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(4 AS DECIMAL(19,0)), 'Maintenance', CAST(4 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(5 AS DECIMAL(19,0)), 'Inventory', CAST(5 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(6 AS DECIMAL(19,0)), 'Procurement', CAST(6 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(7 AS DECIMAL(19,0)), 'Technician', CAST(7 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(8 AS DECIMAL(19,0)), 'Reports', CAST(8 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(9 AS DECIMAL(19,0)), 'Security', CAST(9 AS DECIMAL(10,0)), CAST(1 AS SMALLINT))
) AS src(id, name, sort_order, active)
ON tgt.id = src.id
WHEN MATCHED THEN
    UPDATE SET
        tgt.name = src.name,
        tgt.sort_order = src.sort_order,
        tgt.active = src.active
WHEN NOT MATCHED THEN
    INSERT (id, name, sort_order, active)
    VALUES (src.id, src.name, src.sort_order, src.active);

MERGE dbo.permission_objects AS tgt
USING (VALUES
    (CAST(1 AS DECIMAL(19,0)), CAST(1 AS DECIMAL(19,0)), 'Maintenance Dashboard', 'DASHBOARD', CAST(1 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(2 AS DECIMAL(19,0)), CAST(1 AS DECIMAL(19,0)), 'Security Dashboard', 'DASHBOARD', CAST(1 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(3 AS DECIMAL(19,0)), CAST(1 AS DECIMAL(19,0)), 'Budget Dashboard', 'DASHBOARD', CAST(1 AS SMALLINT), CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(4 AS DECIMAL(19,0)), CAST(2 AS DECIMAL(19,0)), 'Asset Type', 'ASSET_TYPE', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(5 AS DECIMAL(19,0)), CAST(2 AS DECIMAL(19,0)), 'Asset', 'ASSET', CAST(0 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(6 AS DECIMAL(19,0)), CAST(3 AS DECIMAL(19,0)), 'Work Order Type', 'WORK_ORDER_TYPE', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(7 AS DECIMAL(19,0)), CAST(3 AS DECIMAL(19,0)), 'Work Order', 'WORK_ORDER', CAST(0 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(8 AS DECIMAL(19,0)), CAST(4 AS DECIMAL(19,0)), 'Preventive Maintenance', 'PREVENTIVE_MAINTENANCE', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(9 AS DECIMAL(19,0)), CAST(4 AS DECIMAL(19,0)), 'Corrective Maintenance', 'CORRECTIVE_MAINTENANCE', CAST(0 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(10 AS DECIMAL(19,0)), CAST(5 AS DECIMAL(19,0)), 'Warehouse', 'INVENTORY', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(11 AS DECIMAL(19,0)), CAST(5 AS DECIMAL(19,0)), 'Inventory', 'INVENTORY', CAST(0 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(12 AS DECIMAL(19,0)), CAST(5 AS DECIMAL(19,0)), 'Inventory Reconcile', 'INVENTORY', CAST(0 AS SMALLINT), CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(13 AS DECIMAL(19,0)), CAST(5 AS DECIMAL(19,0)), 'Inventory Audit Logs', 'INVENTORY', CAST(0 AS SMALLINT), CAST(4 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(14 AS DECIMAL(19,0)), CAST(6 AS DECIMAL(19,0)), 'Material Requisition', 'MATERIAL_REQUISITION', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(15 AS DECIMAL(19,0)), CAST(6 AS DECIMAL(19,0)), 'Purchase Order', 'PURCHASE_ORDER', CAST(0 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(16 AS DECIMAL(19,0)), CAST(6 AS DECIMAL(19,0)), 'Goods Receipt Notes', 'GOODS_RECEIPT_NOTE', CAST(0 AS SMALLINT), CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(17 AS DECIMAL(19,0)), CAST(7 AS DECIMAL(19,0)), 'Technician', 'TECHNICIAN', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(18 AS DECIMAL(19,0)), CAST(7 AS DECIMAL(19,0)), 'Technician Team', 'TECHNICIAN_TEAM', CAST(0 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(19 AS DECIMAL(19,0)), CAST(8 AS DECIMAL(19,0)), 'Inventory Report', 'REPORTS', CAST(1 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(20 AS DECIMAL(19,0)), CAST(8 AS DECIMAL(19,0)), 'Asset Report', 'REPORTS', CAST(1 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(21 AS DECIMAL(19,0)), CAST(8 AS DECIMAL(19,0)), 'Transaction Report', 'REPORTS', CAST(1 AS SMALLINT), CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(22 AS DECIMAL(19,0)), CAST(8 AS DECIMAL(19,0)), 'Work Order Report', 'REPORTS', CAST(1 AS SMALLINT), CAST(4 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(23 AS DECIMAL(19,0)), CAST(9 AS DECIMAL(19,0)), 'Roles', 'MANAGE_ROLES', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(24 AS DECIMAL(19,0)), CAST(9 AS DECIMAL(19,0)), 'User', 'MANAGE_USERS', CAST(0 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(25 AS DECIMAL(19,0)), CAST(9 AS DECIMAL(19,0)), 'Security Report', 'REPORTS', CAST(1 AS SMALLINT), CAST(4 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
    (CAST(26 AS DECIMAL(19,0)), CAST(9 AS DECIMAL(19,0)), 'MFA', 'MANAGE_USERS', CAST(0 AS SMALLINT), CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT))
) AS src(id, class_id, name, module, view_only, sort_order, active)
ON tgt.id = src.id
WHEN MATCHED THEN
    UPDATE SET
        tgt.class_id = src.class_id,
        tgt.name = src.name,
        tgt.module = src.module,
        tgt.view_only = src.view_only,
        tgt.sort_order = src.sort_order,
        tgt.active = src.active
WHEN NOT MATCHED THEN
    INSERT (id, class_id, name, module, view_only, sort_order, active)
    VALUES (src.id, src.class_id, src.name, src.module, src.view_only, src.sort_order, src.active);
