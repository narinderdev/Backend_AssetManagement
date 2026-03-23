IF OBJECT_ID('dbo.companies', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.companies (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        company_legal_name NVARCHAR(255) NOT NULL,
        company_trade_name NVARCHAR(255) NOT NULL,
        company_number NVARCHAR(100) NOT NULL,
        address NVARCHAR(500) NOT NULL,
        city NVARCHAR(150) NOT NULL,
        country NVARCHAR(150) NOT NULL,
        postal_code NVARCHAR(40) NOT NULL,
        active BIT NOT NULL CONSTRAINT df_companies_active DEFAULT 1,
        created_at DATETIME2 NOT NULL CONSTRAINT df_companies_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_companies_updated_at DEFAULT SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_companies_company_number' AND object_id = OBJECT_ID('dbo.companies'))
BEGIN
    CREATE UNIQUE INDEX uk_companies_company_number ON dbo.companies(company_number);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_companies_company_number' AND object_id = OBJECT_ID('dbo.companies'))
BEGIN
    CREATE INDEX idx_companies_company_number ON dbo.companies(company_number);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_companies_active' AND object_id = OBJECT_ID('dbo.companies'))
BEGIN
    CREATE INDEX idx_companies_active ON dbo.companies(active);
END;
