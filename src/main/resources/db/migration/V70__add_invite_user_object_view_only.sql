IF OBJECT_ID('dbo.app_permissions', 'U') IS NOT NULL
BEGIN
    MERGE dbo.app_permissions AS tgt
    USING (VALUES
        ('view_invite_user', 'INVITE_USER', 'VIEW', 'View Invite User', 'Allows user to view in INVITE_USER', CAST(1 AS BIT), 1005)
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

    -- INVITE_USER should have only VIEW action.
    DECLARE @toRemoveInvite TABLE (id BIGINT);
    INSERT INTO @toRemoveInvite (id)
    SELECT id
    FROM dbo.app_permissions
    WHERE module = 'INVITE_USER'
      AND action <> 'VIEW';

    IF OBJECT_ID('dbo.role_permissions', 'U') IS NOT NULL
    BEGIN
        DELETE rp
        FROM dbo.role_permissions rp
        JOIN @toRemoveInvite r ON r.id = rp.permission_id;
    END;

    DELETE ap
    FROM dbo.app_permissions ap
    JOIN @toRemoveInvite r ON r.id = ap.id;
END;

IF OBJECT_ID('dbo.permission_classes', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.permission_objects', 'U') IS NOT NULL
BEGIN
    DECLARE @securityClassId DECIMAL(19,0);
    SELECT TOP 1 @securityClassId = id
    FROM dbo.permission_classes
    WHERE name = 'Security';

    IF @securityClassId IS NOT NULL
    BEGIN
        -- Upsert Invite User under Security (view-only)
        IF EXISTS (
            SELECT 1
            FROM dbo.permission_objects
            WHERE class_id = @securityClassId
              AND name = 'Invite User'
        )
        BEGIN
            UPDATE dbo.permission_objects
            SET module = 'INVITE_USER',
                view_only = 1,
                active = 1,
                sort_order = 4
            WHERE class_id = @securityClassId
              AND name = 'Invite User';
        END
        ELSE
        BEGIN
            INSERT INTO dbo.permission_objects (id, class_id, name, module, view_only, sort_order, active)
            VALUES (
                (SELECT ISNULL(MAX(id), 0) + 1 FROM dbo.permission_objects),
                @securityClassId,
                'Invite User',
                'INVITE_USER',
                1,
                4,
                1
            );
        END;

        -- Keep Security Report after Invite User
        UPDATE dbo.permission_objects
        SET sort_order = 5
        WHERE class_id = @securityClassId
          AND name = 'Security Report';
    END;
END;
