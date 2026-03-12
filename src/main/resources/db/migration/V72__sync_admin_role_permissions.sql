IF OBJECT_ID('dbo.roles', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.app_permissions', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.role_permissions', 'U') IS NOT NULL
BEGIN
    INSERT INTO dbo.role_permissions (role_id, permission_id)
    SELECT r.id, p.id
    FROM dbo.roles r
    CROSS JOIN dbo.app_permissions p
    WHERE LOWER(r.name) = 'admin'
      AND r.active = 1
      AND p.active = 1
      AND NOT EXISTS (
          SELECT 1
          FROM dbo.role_permissions rp
          WHERE rp.role_id = r.id
            AND rp.permission_id = p.id
      );
END;
