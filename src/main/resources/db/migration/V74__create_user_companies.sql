IF OBJECT_ID('dbo.user_companies', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.user_companies (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        company_id BIGINT NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_user_companies_created_at DEFAULT SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_user_companies_user_company' AND object_id = OBJECT_ID('dbo.user_companies'))
BEGIN
    CREATE UNIQUE INDEX uk_user_companies_user_company ON dbo.user_companies(user_id, company_id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_user_companies_user_id' AND object_id = OBJECT_ID('dbo.user_companies'))
BEGIN
    CREATE INDEX idx_user_companies_user_id ON dbo.user_companies(user_id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_user_companies_company_id' AND object_id = OBJECT_ID('dbo.user_companies'))
BEGIN
    CREATE INDEX idx_user_companies_company_id ON dbo.user_companies(company_id);
END;

IF OBJECT_ID('dbo.users', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_user_companies_user' AND parent_object_id = OBJECT_ID('dbo.user_companies'))
BEGIN
    ALTER TABLE dbo.user_companies
        ADD CONSTRAINT fk_user_companies_user FOREIGN KEY (user_id) REFERENCES dbo.users(id);
END;

IF OBJECT_ID('dbo.companies', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_user_companies_company' AND parent_object_id = OBJECT_ID('dbo.user_companies'))
BEGIN
    ALTER TABLE dbo.user_companies
        ADD CONSTRAINT fk_user_companies_company FOREIGN KEY (company_id) REFERENCES dbo.companies(id);
END;
