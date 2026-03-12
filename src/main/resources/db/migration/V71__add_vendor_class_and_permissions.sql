IF OBJECT_ID('dbo.permission_classes', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.permission_objects', 'U') IS NOT NULL
BEGIN
    -- Normalize class order (Vendor as its own class)
    UPDATE dbo.permission_classes SET sort_order = 1, active = 1 WHERE name = 'Dashboard';
    UPDATE dbo.permission_classes SET sort_order = 2, active = 1 WHERE name = 'Asset';
    UPDATE dbo.permission_classes SET sort_order = 3, active = 1 WHERE name = 'Service Request';
    UPDATE dbo.permission_classes SET sort_order = 4, active = 1 WHERE name = 'Work Order';
    UPDATE dbo.permission_classes SET sort_order = 5, active = 1 WHERE name = 'Maintenance';
    UPDATE dbo.permission_classes SET sort_order = 6, active = 1 WHERE name = 'Inventory';
    UPDATE dbo.permission_classes SET sort_order = 7, active = 1 WHERE name = 'Procurement';
    UPDATE dbo.permission_classes SET sort_order = 9, active = 1 WHERE name = 'Technician';
    UPDATE dbo.permission_classes SET sort_order = 10, active = 1 WHERE name = 'Reports';
    UPDATE dbo.permission_classes SET sort_order = 11, active = 1 WHERE name = 'Security';

    DECLARE @vendorClassId DECIMAL(19,0);
    SELECT TOP 1 @vendorClassId = id
    FROM dbo.permission_classes
    WHERE name = 'Vendor';

    IF @vendorClassId IS NULL
    BEGIN
        IF COLUMNPROPERTY(OBJECT_ID('dbo.permission_classes'), 'id', 'IsIdentity') = 1
        BEGIN
            INSERT INTO dbo.permission_classes (name, sort_order, active)
            VALUES ('Vendor', 8, 1);
            SET @vendorClassId = CAST(SCOPE_IDENTITY() AS DECIMAL(19,0));
        END
        ELSE
        BEGIN
            SELECT @vendorClassId = ISNULL(MAX(id), 0) + 1
            FROM dbo.permission_classes;

            INSERT INTO dbo.permission_classes (id, name, sort_order, active)
            VALUES (@vendorClassId, 'Vendor', 8, 1);
        END
    END
    ELSE
    BEGIN
        UPDATE dbo.permission_classes
        SET sort_order = 8,
            active = 1
        WHERE id = @vendorClassId;
    END;

    -- Vendor should no longer be listed under Procurement class.
    UPDATE po
    SET po.active = 0
    FROM dbo.permission_objects po
    JOIN dbo.permission_classes pc ON pc.id = po.class_id
    WHERE pc.name = 'Procurement'
      AND po.name = 'Vendor';

    -- Ensure Vendor object exists under Vendor class.
    IF EXISTS (
        SELECT 1
        FROM dbo.permission_objects
        WHERE class_id = @vendorClassId
          AND name = 'Vendor'
    )
    BEGIN
        UPDATE dbo.permission_objects
        SET module = 'VENDOR',
            view_only = 0,
            sort_order = 1,
            active = 1
        WHERE class_id = @vendorClassId
          AND name = 'Vendor';
    END
    ELSE
    BEGIN
        IF COLUMNPROPERTY(OBJECT_ID('dbo.permission_objects'), 'id', 'IsIdentity') = 1
        BEGIN
            INSERT INTO dbo.permission_objects (class_id, name, module, view_only, sort_order, active)
            VALUES (@vendorClassId, 'Vendor', 'VENDOR', 0, 1, 1);
        END
        ELSE
        BEGIN
            DECLARE @vendorObjectId DECIMAL(19,0);
            SELECT @vendorObjectId = ISNULL(MAX(id), 0) + 1
            FROM dbo.permission_objects;

            INSERT INTO dbo.permission_objects (id, class_id, name, module, view_only, sort_order, active)
            VALUES (@vendorObjectId, @vendorClassId, 'Vendor', 'VENDOR', 0, 1, 1);
        END
    END;
END;

IF OBJECT_ID('dbo.app_permissions', 'U') IS NOT NULL
BEGIN
    MERGE dbo.app_permissions AS tgt
    USING (VALUES
        ('create_vendor', 'VENDOR', 'CREATE', 'Create Vendor', 'Allows user to create in VENDOR', CAST(1 AS BIT), 1201),
        ('view_vendor', 'VENDOR', 'VIEW', 'View Vendor', 'Allows user to view in VENDOR', CAST(1 AS BIT), 1202),
        ('update_vendor', 'VENDOR', 'UPDATE', 'Update Vendor', 'Allows user to update in VENDOR', CAST(1 AS BIT), 1203),
        ('delete_vendor', 'VENDOR', 'DELETE', 'Delete Vendor', 'Allows user to delete in VENDOR', CAST(1 AS BIT), 1204),
        ('approve_vendor', 'VENDOR', 'APPROVE', 'Approve Vendor', 'Allows user to approve in VENDOR', CAST(1 AS BIT), 1205)
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
END;
