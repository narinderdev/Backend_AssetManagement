-- Ensure Dashboard and Reports have only VIEW permissions by deleting other actions if they exist

DECLARE @permIds TABLE (id BIGINT);

IF OBJECT_ID('dbo.app_permissions', 'U') IS NOT NULL
BEGIN
    INSERT INTO @permIds (id)
    SELECT id
    FROM dbo.app_permissions
    WHERE code IN (
        'create_dashboard',
        'update_dashboard',
        'delete_dashboard',
        'create_reports',
        'update_reports',
        'delete_reports'
    );

    IF OBJECT_ID('dbo.role_permissions', 'U') IS NOT NULL
    BEGIN
        DELETE rp
        FROM dbo.role_permissions rp
        JOIN @permIds p ON rp.permission_id = p.id;
    END;

    DELETE ap
    FROM dbo.app_permissions ap
    JOIN @permIds p ON ap.id = p.id;
END;
