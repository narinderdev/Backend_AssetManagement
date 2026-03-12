IF OBJECT_ID('dbo.permission_classes', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.permission_objects', 'U') IS NOT NULL
BEGIN
    MERGE dbo.permission_classes AS tgt
    USING (VALUES
        (CAST(1 AS DECIMAL(19,0)), 'Dashboard', CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(2 AS DECIMAL(19,0)), 'Asset', CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(10 AS DECIMAL(19,0)), 'Service Request', CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(3 AS DECIMAL(19,0)), 'Work Order', CAST(4 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(4 AS DECIMAL(19,0)), 'Maintenance', CAST(5 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(5 AS DECIMAL(19,0)), 'Inventory', CAST(6 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(6 AS DECIMAL(19,0)), 'Procurement', CAST(7 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(7 AS DECIMAL(19,0)), 'Technician', CAST(8 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(8 AS DECIMAL(19,0)), 'Reports', CAST(9 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(9 AS DECIMAL(19,0)), 'Security', CAST(10 AS DECIMAL(10,0)), CAST(1 AS SMALLINT))
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

    -- Backward compatibility in case older row name still exists
    UPDATE po
    SET po.name = 'User', po.module = 'MANAGE_USERS', po.view_only = 0
    FROM dbo.permission_objects po
    WHERE po.name = 'User MFA';

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
        (CAST(19 AS DECIMAL(19,0)), CAST(8 AS DECIMAL(19,0)), 'Inventory Report', 'REPORTS', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(20 AS DECIMAL(19,0)), CAST(8 AS DECIMAL(19,0)), 'Asset Report', 'REPORTS', CAST(0 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(21 AS DECIMAL(19,0)), CAST(8 AS DECIMAL(19,0)), 'Transaction Report', 'REPORTS', CAST(0 AS SMALLINT), CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(22 AS DECIMAL(19,0)), CAST(8 AS DECIMAL(19,0)), 'Work Order Report', 'REPORTS', CAST(0 AS SMALLINT), CAST(4 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(23 AS DECIMAL(19,0)), CAST(9 AS DECIMAL(19,0)), 'Roles', 'MANAGE_ROLES', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(24 AS DECIMAL(19,0)), CAST(9 AS DECIMAL(19,0)), 'User', 'MANAGE_USERS', CAST(0 AS SMALLINT), CAST(2 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(25 AS DECIMAL(19,0)), CAST(9 AS DECIMAL(19,0)), 'Security Report', 'REPORTS', CAST(0 AS SMALLINT), CAST(4 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(26 AS DECIMAL(19,0)), CAST(9 AS DECIMAL(19,0)), 'MFA', 'MANAGE_USERS', CAST(0 AS SMALLINT), CAST(3 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(27 AS DECIMAL(19,0)), CAST(10 AS DECIMAL(19,0)), 'Service Request', 'SERVICE_REQUEST', CAST(0 AS SMALLINT), CAST(1 AS DECIMAL(10,0)), CAST(1 AS SMALLINT)),
        (CAST(28 AS DECIMAL(19,0)), CAST(6 AS DECIMAL(19,0)), 'Vendor', 'VENDOR', CAST(0 AS SMALLINT), CAST(4 AS DECIMAL(10,0)), CAST(1 AS SMALLINT))
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
END;
