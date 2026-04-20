-- Scope technician directory uniqueness by company.
-- This allows same email/team name in different companies.

IF OBJECT_ID('dbo.technicians', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('technicians', 'company_id') IS NULL
    BEGIN
        ALTER TABLE dbo.technicians ADD company_id BIGINT NULL;
    END;

    IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uk_technicians_email' AND parent_object_id = OBJECT_ID('dbo.technicians'))
        ALTER TABLE dbo.technicians DROP CONSTRAINT uk_technicians_email;
    IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uk_technicians_badge' AND parent_object_id = OBJECT_ID('dbo.technicians'))
        ALTER TABLE dbo.technicians DROP CONSTRAINT uk_technicians_badge;
    IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uk_technicians_identifier' AND parent_object_id = OBJECT_ID('dbo.technicians'))
        ALTER TABLE dbo.technicians DROP CONSTRAINT uk_technicians_identifier;

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_technicians_email' AND object_id = OBJECT_ID('dbo.technicians'))
        DROP INDEX uk_technicians_email ON dbo.technicians;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_technicians_badge' AND object_id = OBJECT_ID('dbo.technicians'))
        DROP INDEX uk_technicians_badge ON dbo.technicians;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_technicians_identifier' AND object_id = OBJECT_ID('dbo.technicians'))
        DROP INDEX uk_technicians_identifier ON dbo.technicians;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_technicians_badge' AND object_id = OBJECT_ID('dbo.technicians'))
        DROP INDEX ux_technicians_badge ON dbo.technicians;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_technicians_identifier' AND object_id = OBJECT_ID('dbo.technicians'))
        DROP INDEX ux_technicians_identifier ON dbo.technicians;

    DECLARE @dropTechnicianConstraintSql NVARCHAR(MAX) = N'';
    SELECT @dropTechnicianConstraintSql = @dropTechnicianConstraintSql + N'ALTER TABLE dbo.technicians DROP CONSTRAINT [' + kc.name + N'];'
    FROM sys.key_constraints kc
    JOIN sys.indexes i
      ON i.object_id = kc.parent_object_id
     AND i.index_id = kc.unique_index_id
    JOIN sys.index_columns ic
      ON ic.object_id = i.object_id
     AND ic.index_id = i.index_id
    JOIN sys.columns c
      ON c.object_id = ic.object_id
     AND c.column_id = ic.column_id
    WHERE kc.parent_object_id = OBJECT_ID('dbo.technicians')
      AND kc.type = 'UQ'
    GROUP BY kc.name
    HAVING COUNT(*) = 1 AND MAX(c.name) IN ('email', 'badge_number', 'technician_id');

    IF LEN(@dropTechnicianConstraintSql) > 0
        EXEC sp_executesql @dropTechnicianConstraintSql;

    DECLARE @dropTechnicianIndexSql NVARCHAR(MAX) = N'';
    SELECT @dropTechnicianIndexSql = @dropTechnicianIndexSql + N'DROP INDEX [' + i.name + N'] ON dbo.technicians;'
    FROM sys.indexes i
    JOIN sys.index_columns ic
      ON ic.object_id = i.object_id
     AND ic.index_id = i.index_id
    JOIN sys.columns c
      ON c.object_id = ic.object_id
     AND c.column_id = ic.column_id
    WHERE i.object_id = OBJECT_ID('dbo.technicians')
      AND i.is_unique = 1
      AND i.is_primary_key = 0
      AND i.is_unique_constraint = 0
    GROUP BY i.name
    HAVING COUNT(*) = 1 AND MAX(c.name) IN ('email', 'badge_number', 'technician_id');

    IF LEN(@dropTechnicianIndexSql) > 0
        EXEC sp_executesql @dropTechnicianIndexSql;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_technicians_company_email' AND object_id = OBJECT_ID('dbo.technicians'))
        CREATE UNIQUE INDEX uk_technicians_company_email ON dbo.technicians(company_id, email);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_technicians_company_badge' AND object_id = OBJECT_ID('dbo.technicians'))
        CREATE UNIQUE INDEX uk_technicians_company_badge ON dbo.technicians(company_id, badge_number);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_technicians_company_identifier' AND object_id = OBJECT_ID('dbo.technicians'))
        CREATE UNIQUE INDEX uk_technicians_company_identifier ON dbo.technicians(company_id, technician_id);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_technicians_company_id' AND object_id = OBJECT_ID('dbo.technicians'))
        CREATE INDEX idx_technicians_company_id ON dbo.technicians(company_id);
END;

IF OBJECT_ID('dbo.technician_teams', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('technician_teams', 'company_id') IS NULL
    BEGIN
        ALTER TABLE dbo.technician_teams ADD company_id BIGINT NULL;
    END;

    IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uk_technician_teams_team_name' AND parent_object_id = OBJECT_ID('dbo.technician_teams'))
        ALTER TABLE dbo.technician_teams DROP CONSTRAINT uk_technician_teams_team_name;

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_technician_teams_team_name' AND object_id = OBJECT_ID('dbo.technician_teams'))
        DROP INDEX uk_technician_teams_team_name ON dbo.technician_teams;

    DECLARE @dropTeamConstraintSql NVARCHAR(MAX) = N'';
    SELECT @dropTeamConstraintSql = @dropTeamConstraintSql + N'ALTER TABLE dbo.technician_teams DROP CONSTRAINT [' + kc.name + N'];'
    FROM sys.key_constraints kc
    JOIN sys.indexes i
      ON i.object_id = kc.parent_object_id
     AND i.index_id = kc.unique_index_id
    JOIN sys.index_columns ic
      ON ic.object_id = i.object_id
     AND ic.index_id = i.index_id
    JOIN sys.columns c
      ON c.object_id = ic.object_id
     AND c.column_id = ic.column_id
    WHERE kc.parent_object_id = OBJECT_ID('dbo.technician_teams')
      AND kc.type = 'UQ'
    GROUP BY kc.name
    HAVING COUNT(*) = 1 AND MAX(c.name) = 'team_name';

    IF LEN(@dropTeamConstraintSql) > 0
        EXEC sp_executesql @dropTeamConstraintSql;

    DECLARE @dropTeamIndexSql NVARCHAR(MAX) = N'';
    SELECT @dropTeamIndexSql = @dropTeamIndexSql + N'DROP INDEX [' + i.name + N'] ON dbo.technician_teams;'
    FROM sys.indexes i
    JOIN sys.index_columns ic
      ON ic.object_id = i.object_id
     AND ic.index_id = i.index_id
    JOIN sys.columns c
      ON c.object_id = ic.object_id
     AND c.column_id = ic.column_id
    WHERE i.object_id = OBJECT_ID('dbo.technician_teams')
      AND i.is_unique = 1
      AND i.is_primary_key = 0
      AND i.is_unique_constraint = 0
    GROUP BY i.name
    HAVING COUNT(*) = 1 AND MAX(c.name) = 'team_name';

    IF LEN(@dropTeamIndexSql) > 0
        EXEC sp_executesql @dropTeamIndexSql;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_technician_teams_company_team_name' AND object_id = OBJECT_ID('dbo.technician_teams'))
        CREATE UNIQUE INDEX uk_technician_teams_company_team_name ON dbo.technician_teams(company_id, team_name);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_technician_teams_company_id' AND object_id = OBJECT_ID('dbo.technician_teams'))
        CREATE INDEX idx_technician_teams_company_id ON dbo.technician_teams(company_id);
END;
