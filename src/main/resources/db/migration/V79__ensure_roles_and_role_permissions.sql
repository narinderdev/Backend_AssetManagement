-- Ensure role tables exist for startup seeders after clean databases.

IF OBJECT_ID('dbo.roles', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.roles (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NULL,
        name NVARCHAR(80) NOT NULL,
        description NVARCHAR(500) NULL,
        active BIT NOT NULL CONSTRAINT df_roles_active DEFAULT 1,
        technician_role BIT NOT NULL CONSTRAINT df_roles_technician_role DEFAULT 0,
        created_at DATETIME2 NOT NULL CONSTRAINT df_roles_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_roles_updated_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF OBJECT_ID('dbo.role_permissions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.role_permissions (
        role_id BIGINT NOT NULL,
        permission_id BIGINT NOT NULL
    );
END;
GO

IF OBJECT_ID('dbo.roles', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_role_active'
         AND object_id = OBJECT_ID('dbo.roles')
   )
BEGIN
    CREATE INDEX idx_role_active ON dbo.roles(active);
END;
GO

IF OBJECT_ID('dbo.roles', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_role_name'
         AND object_id = OBJECT_ID('dbo.roles')
   )
BEGIN
    CREATE INDEX idx_role_name ON dbo.roles(name);
END;
GO

IF OBJECT_ID('dbo.roles', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_role_company_id'
         AND object_id = OBJECT_ID('dbo.roles')
   )
BEGIN
    CREATE INDEX idx_role_company_id ON dbo.roles(company_id);
END;
GO

IF OBJECT_ID('dbo.roles', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'uk_role_company_name'
         AND object_id = OBJECT_ID('dbo.roles')
   )
BEGIN
    CREATE UNIQUE INDEX uk_role_company_name ON dbo.roles(company_id, name);
END;
GO

IF OBJECT_ID('dbo.role_permissions', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'uk_role_permission'
         AND object_id = OBJECT_ID('dbo.role_permissions')
   )
BEGIN
    CREATE UNIQUE INDEX uk_role_permission ON dbo.role_permissions(role_id, permission_id);
END;
GO

IF OBJECT_ID('dbo.role_permissions', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE name = 'idx_role_permissions_permission_id'
         AND object_id = OBJECT_ID('dbo.role_permissions')
   )
BEGIN
    CREATE INDEX idx_role_permissions_permission_id ON dbo.role_permissions(permission_id);
END;
GO

IF OBJECT_ID('dbo.companies', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.roles', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_roles_company_id'
         AND parent_object_id = OBJECT_ID('dbo.roles')
   )
BEGIN
    ALTER TABLE dbo.roles
        ADD CONSTRAINT fk_roles_company_id FOREIGN KEY (company_id) REFERENCES dbo.companies(id);
END;
GO

IF OBJECT_ID('dbo.role_permissions', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.roles', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_role_permissions_role'
         AND parent_object_id = OBJECT_ID('dbo.role_permissions')
   )
BEGIN
    ALTER TABLE dbo.role_permissions
        ADD CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id);
END;
GO

IF OBJECT_ID('dbo.role_permissions', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.app_permissions', 'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.foreign_keys
       WHERE name = 'fk_role_permissions_permission'
         AND parent_object_id = OBJECT_ID('dbo.role_permissions')
   )
BEGIN
    ALTER TABLE dbo.role_permissions
        ADD CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES dbo.app_permissions(id);
END;
GO
