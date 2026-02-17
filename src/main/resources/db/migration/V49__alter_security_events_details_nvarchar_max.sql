-- Align security_events.details type with entity mapping

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.objects o ON c.object_id = o.object_id
    WHERE o.name = 'security_events'
      AND c.name = 'details'
      AND c.system_type_id = TYPE_ID('varchar')
)
BEGIN
    ALTER TABLE security_events ALTER COLUMN details NVARCHAR(MAX) NULL;
END;

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.objects o ON c.object_id = o.object_id
    WHERE o.name = 'security_events'
      AND c.name = 'details'
      AND c.system_type_id = TYPE_ID('nvarchar')
      AND c.max_length <> -1
)
BEGIN
    ALTER TABLE security_events ALTER COLUMN details NVARCHAR(MAX) NULL;
END;
