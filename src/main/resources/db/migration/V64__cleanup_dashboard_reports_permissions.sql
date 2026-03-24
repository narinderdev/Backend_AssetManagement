IF OBJECT_ID('dbo.app_permissions', 'U') IS NOT NULL
BEGIN
    IF OBJECT_ID('dbo.role_permissions', 'U') IS NOT NULL
    BEGIN
        DELETE FROM dbo.role_permissions
        WHERE permission_id IN (
            SELECT id
            FROM dbo.app_permissions
            WHERE module IN ('DASHBOARD', 'REPORTS')
              AND action <> 'VIEW'
        );
    END;

    DELETE FROM dbo.app_permissions
    WHERE module IN ('DASHBOARD', 'REPORTS')
      AND action <> 'VIEW';
END;
