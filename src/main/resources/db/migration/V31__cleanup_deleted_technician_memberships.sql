-- Remove team memberships that point to soft-deleted technicians
DELETE ttm
FROM technician_team_members ttm
WHERE EXISTS (
    SELECT 1
    FROM technicians t
    WHERE t.id = ttm.technician_id
      AND t.is_deleted = 1
);
