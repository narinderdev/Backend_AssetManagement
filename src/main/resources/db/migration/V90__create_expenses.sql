IF OBJECT_ID('dbo.expenses', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.expenses (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        company_id BIGINT NOT NULL,
        expense_code NVARCHAR(64) NOT NULL,
        expense_type NVARCHAR(100) NOT NULL,
        notes NVARCHAR(1000) NULL,
        expense_date DATE NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_expenses_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
END;

IF OBJECT_ID('dbo.expenses', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_expenses_company_expense_code' AND object_id = OBJECT_ID('dbo.expenses'))
BEGIN
    CREATE UNIQUE INDEX ux_expenses_company_expense_code ON dbo.expenses(company_id, expense_code);
END;

IF OBJECT_ID('dbo.expenses', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_expenses_company_expense_date' AND object_id = OBJECT_ID('dbo.expenses'))
BEGIN
    CREATE INDEX idx_expenses_company_expense_date ON dbo.expenses(company_id, expense_date DESC);
END;

IF OBJECT_ID('dbo.expenses', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.companies', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_expenses_company')
BEGIN
    ALTER TABLE dbo.expenses
        ADD CONSTRAINT fk_expenses_company FOREIGN KEY (company_id) REFERENCES dbo.companies(id);
END;
