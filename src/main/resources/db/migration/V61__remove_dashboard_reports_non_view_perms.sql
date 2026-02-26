-- Ensure Dashboard and Reports have only VIEW permissions by deleting other actions if they exist

DECLARE @permIds TABLE (id BIGINT);

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

DELETE rp
FROM dbo.role_permissions rp
JOIN @permIds p ON rp.permission_id = p.id;

DELETE ap
FROM dbo.app_permissions ap
JOIN @permIds p ON ap.id = p.id;
