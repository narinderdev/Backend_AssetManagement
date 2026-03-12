IF OBJECT_ID('dbo.permission_classes', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.permission_objects', 'U') IS NOT NULL
BEGIN
    DECLARE @securityClassId DECIMAL(19,0);
    SELECT TOP 1 @securityClassId = id
    FROM dbo.permission_classes
    WHERE name = 'Security';

    IF @securityClassId IS NOT NULL
    BEGIN
        -- Backward compatibility: rename old combined object
        UPDATE dbo.permission_objects
        SET name = 'User',
            module = 'MANAGE_USERS',
            view_only = 0
        WHERE class_id = @securityClassId
          AND name = 'User MFA';

        -- Ensure User object exists
        IF NOT EXISTS (
            SELECT 1
            FROM dbo.permission_objects
            WHERE class_id = @securityClassId
              AND name = 'User'
        )
        BEGIN
            INSERT INTO dbo.permission_objects (id, class_id, name, module, view_only, sort_order, active)
            VALUES (
                (SELECT ISNULL(MAX(id), 0) + 1 FROM dbo.permission_objects),
                @securityClassId,
                'User',
                'MANAGE_USERS',
                0,
                2,
                1
            );
        END;

        -- Ensure MFA object exists
        IF NOT EXISTS (
            SELECT 1
            FROM dbo.permission_objects
            WHERE class_id = @securityClassId
              AND name = 'MFA'
        )
        BEGIN
            INSERT INTO dbo.permission_objects (id, class_id, name, module, view_only, sort_order, active)
            VALUES (
                (SELECT ISNULL(MAX(id), 0) + 1 FROM dbo.permission_objects),
                @securityClassId,
                'MFA',
                'MANAGE_USERS',
                0,
                3,
                1
            );
        END;

        -- Normalize order under Security class
        UPDATE dbo.permission_objects
        SET sort_order = 1
        WHERE class_id = @securityClassId
          AND name = 'Roles';

        UPDATE dbo.permission_objects
        SET sort_order = 2
        WHERE class_id = @securityClassId
          AND name = 'User';

        UPDATE dbo.permission_objects
        SET sort_order = 3
        WHERE class_id = @securityClassId
          AND name = 'MFA';

        UPDATE dbo.permission_objects
        SET sort_order = 4
        WHERE class_id = @securityClassId
          AND name = 'Security Report';
    END;
END;
