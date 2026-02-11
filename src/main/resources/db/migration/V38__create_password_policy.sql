-- Central password policy table (single row)
IF OBJECT_ID('password_policy', 'U') IS NULL
BEGIN
    CREATE TABLE password_policy (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        password_expiry_days INT NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_password_policy_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_password_policy_updated DEFAULT SYSUTCDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM password_policy)
BEGIN
    INSERT INTO password_policy (password_expiry_days) VALUES (90);
END;
