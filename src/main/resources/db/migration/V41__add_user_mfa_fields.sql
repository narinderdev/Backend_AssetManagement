IF COL_LENGTH('users', 'mfa_enabled') IS NULL
BEGIN
    ALTER TABLE users
        ADD mfa_enabled BIT NOT NULL CONSTRAINT df_users_mfa_enabled DEFAULT 0;
END;

IF COL_LENGTH('users', 'mfa_secret') IS NULL
BEGIN
    ALTER TABLE users
        ADD mfa_secret NVARCHAR(512) NULL;
END;

IF COL_LENGTH('users', 'mfa_secret_temp') IS NULL
BEGIN
    ALTER TABLE users
        ADD mfa_secret_temp NVARCHAR(512) NULL;
END;
