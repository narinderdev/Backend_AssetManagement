IF OBJECT_ID('dbo.app_permissions', 'U') IS NOT NULL
BEGIN
    MERGE dbo.app_permissions AS tgt
    USING (VALUES
        ('approve_service_request', 'SERVICE_REQUEST', 'APPROVE', 'Approve Service Request', 'Allows user to approve in SERVICE_REQUEST', CAST(1 AS BIT), 1001),
        ('approve_work_order', 'WORK_ORDER', 'APPROVE', 'Approve Work Order', 'Allows user to approve in WORK_ORDER', CAST(1 AS BIT), 1002),
        ('approve_vendor', 'VENDOR', 'APPROVE', 'Approve Vendor', 'Allows user to approve in VENDOR', CAST(1 AS BIT), 1003),
        ('export_reports', 'REPORTS', 'EXPORT', 'Export Reports', 'Allows user to export in REPORTS', CAST(1 AS BIT), 1004)
    ) AS src(code, module, action, label, description, active, sort_order)
    ON tgt.code = src.code
    WHEN MATCHED THEN
        UPDATE SET
            tgt.module = src.module,
            tgt.action = src.action,
            tgt.label = src.label,
            tgt.description = src.description,
            tgt.active = src.active,
            tgt.sort_order = src.sort_order
    WHEN NOT MATCHED THEN
        INSERT (code, module, action, label, description, active, sort_order)
        VALUES (src.code, src.module, src.action, src.label, src.description, src.active, src.sort_order);

    -- REPORTS should expose only VIEW and EXPORT.
    DECLARE @toRemove TABLE (id BIGINT);
    INSERT INTO @toRemove (id)
    SELECT id
    FROM dbo.app_permissions
    WHERE module = 'REPORTS'
      AND action NOT IN ('VIEW', 'EXPORT');

    DELETE rp
    FROM dbo.role_permissions rp
    JOIN @toRemove r ON r.id = rp.permission_id;

    DELETE ap
    FROM dbo.app_permissions ap
    JOIN @toRemove r ON r.id = ap.id;
END;
