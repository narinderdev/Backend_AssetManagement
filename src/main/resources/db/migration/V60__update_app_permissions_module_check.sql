-- Update app_permissions.module check constraint to include new modules added to PermissionModule enum

DECLARE @constraintName sysname;

IF OBJECT_ID('dbo.app_permissions', 'U') IS NOT NULL
BEGIN
    SELECT TOP 1 @constraintName = dc.name
    FROM sys.check_constraints dc
    WHERE dc.parent_object_id = OBJECT_ID('dbo.app_permissions')
      AND dc.definition LIKE '%module%';

    IF @constraintName IS NOT NULL
    BEGIN
        DECLARE @sql NVARCHAR(400);
        SET @sql = N'ALTER TABLE dbo.app_permissions DROP CONSTRAINT [' + @constraintName + N']';
        EXEC sp_executesql @sql;
    END;

    ALTER TABLE dbo.app_permissions WITH CHECK ADD CONSTRAINT CK_app_permissions_module
    CHECK (module IN (
        'ASSET',
        'ASSET_TYPE',
        'SERVICE_REQUEST',
        'WORK_ORDER',
        'WORK_ORDER_TYPE',
        'CORRECTIVE_MAINTENANCE',
        'PREVENTIVE_MAINTENANCE',
        'MATERIAL_REQUISITION',
        'PURCHASE_ORDER',
        'GOODS_RECEIPT_NOTE',
        'VENDOR',
        'INVENTORY',
        'TECHNICIAN',
        'TECHNICIAN_TEAM',
        'DASHBOARD',
        'REPORTS',
        'MANAGE_USERS',
        'MANAGE_ROLES',
        'INVITE_USER'
    ));

    ALTER TABLE dbo.app_permissions CHECK CONSTRAINT CK_app_permissions_module;
END;
