-- Update app_permissions.action check constraint to include APPROVE and EXPORT

DECLARE @constraintName sysname;

SELECT TOP 1 @constraintName = dc.name
FROM sys.check_constraints dc
WHERE dc.parent_object_id = OBJECT_ID('dbo.app_permissions')
  AND dc.definition LIKE '%action%'
  AND dc.definition LIKE '%CREATE%'
  AND dc.definition LIKE '%VIEW%'
  AND dc.definition LIKE '%UPDATE%'
  AND dc.definition LIKE '%DELETE%';

IF @constraintName IS NOT NULL
BEGIN
    DECLARE @sql NVARCHAR(400);
    SET @sql = N'ALTER TABLE dbo.app_permissions DROP CONSTRAINT [' + @constraintName + N']';
    EXEC sp_executesql @sql;
END;

ALTER TABLE dbo.app_permissions WITH CHECK ADD CONSTRAINT CK_app_permissions_action
CHECK (action IN (
    'CREATE',
    'VIEW',
    'UPDATE',
    'DELETE',
    'APPROVE',
    'EXPORT',
    'ACCESS'
));

ALTER TABLE dbo.app_permissions CHECK CONSTRAINT CK_app_permissions_action;
