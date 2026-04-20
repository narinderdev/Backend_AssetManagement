IF OBJECT_ID('dbo.expenses', 'U') IS NOT NULL
   AND EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_expenses_company_expense_code' AND object_id = OBJECT_ID('dbo.expenses'))
BEGIN
    DROP INDEX ux_expenses_company_expense_code ON dbo.expenses;
END;
