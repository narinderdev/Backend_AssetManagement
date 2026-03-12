DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT id
    FROM app_permissions
    WHERE module IN ('DASHBOARD', 'REPORTS')
      AND action <> 'VIEW'
);

DELETE FROM app_permissions
WHERE module IN ('DASHBOARD', 'REPORTS')
  AND action <> 'VIEW';
