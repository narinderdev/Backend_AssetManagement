IF OBJECT_ID('dbo.permission_objects', 'U') IS NOT NULL
BEGIN
    -- Rename "Preventive Maintenance" -> "Preventive"
    IF EXISTS (
        SELECT 1
        FROM dbo.permission_objects
        WHERE name = 'Preventive Maintenance'
          AND module = 'PREVENTIVE_MAINTENANCE'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM dbo.permission_objects
        WHERE name = 'Preventive'
          AND module = 'PREVENTIVE_MAINTENANCE'
    )
    BEGIN
        UPDATE dbo.permission_objects
        SET name = 'Preventive'
        WHERE name = 'Preventive Maintenance'
          AND module = 'PREVENTIVE_MAINTENANCE';
    END;

    -- Rename "Corrective Maintenance" -> "Corrective"
    IF EXISTS (
        SELECT 1
        FROM dbo.permission_objects
        WHERE name = 'Corrective Maintenance'
          AND module = 'CORRECTIVE_MAINTENANCE'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM dbo.permission_objects
        WHERE name = 'Corrective'
          AND module = 'CORRECTIVE_MAINTENANCE'
    )
    BEGIN
        UPDATE dbo.permission_objects
        SET name = 'Corrective'
        WHERE name = 'Corrective Maintenance'
          AND module = 'CORRECTIVE_MAINTENANCE';
    END;
END;
