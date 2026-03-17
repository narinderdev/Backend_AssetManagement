-- Central password policy table (single row)
IF OBJECT_ID('dbo.password_policy', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.password_policy (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        password_expiry_days INT NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_password_policy_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_password_policy_updated DEFAULT SYSUTCDATETIME()
    );
END;

IF COL_LENGTH('dbo.password_policy', 'created_at') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.default_constraints dc
       INNER JOIN sys.columns c ON c.default_object_id = dc.object_id
       WHERE dc.parent_object_id = OBJECT_ID('dbo.password_policy')
         AND c.name = 'created_at'
   )
BEGIN
    ALTER TABLE dbo.password_policy
    ADD CONSTRAINT df_password_policy_created DEFAULT SYSUTCDATETIME() FOR created_at;
END;

IF COL_LENGTH('dbo.password_policy', 'updated_at') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.default_constraints dc
       INNER JOIN sys.columns c ON c.default_object_id = dc.object_id
       WHERE dc.parent_object_id = OBJECT_ID('dbo.password_policy')
         AND c.name = 'updated_at'
   )
BEGIN
    ALTER TABLE dbo.password_policy
    ADD CONSTRAINT df_password_policy_updated DEFAULT SYSUTCDATETIME() FOR updated_at;
END;

IF NOT EXISTS (SELECT 1 FROM dbo.password_policy)
BEGIN
    INSERT INTO dbo.password_policy (password_expiry_days, created_at, updated_at)
    VALUES (90, SYSUTCDATETIME(), SYSUTCDATETIME());
END;
