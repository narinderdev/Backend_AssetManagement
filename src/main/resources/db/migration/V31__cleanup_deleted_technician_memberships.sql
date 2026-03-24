-- Remove team memberships that point to soft-deleted technicians
IF OBJECT_ID('dbo.technician_team_members', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.technicians', 'U') IS NOT NULL
BEGIN
    DELETE ttm
    FROM dbo.technician_team_members ttm
    WHERE EXISTS (
        SELECT 1
        FROM dbo.technicians t
        WHERE t.id = ttm.technician_id
          AND t.is_deleted = 1
    );
END;
