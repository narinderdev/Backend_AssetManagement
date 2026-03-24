IF OBJECT_ID('users', 'U') IS NOT NULL AND COL_LENGTH('users', 'mfa_email_otp') IS NULL
BEGIN
    ALTER TABLE users
        ADD mfa_email_otp NVARCHAR(10) NULL;
END;
IF OBJECT_ID('users', 'U') IS NOT NULL AND COL_LENGTH('users', 'mfa_email_otp_expires_at') IS NULL
BEGIN
    ALTER TABLE users
        ADD mfa_email_otp_expires_at DATETIME2 NULL;
END;
IF OBJECT_ID('users', 'U') IS NOT NULL AND COL_LENGTH('users', 'mfa_email_verified') IS NULL
BEGIN
    ALTER TABLE users
        ADD mfa_email_verified BIT NOT NULL CONSTRAINT df_users_mfa_email_verified DEFAULT 0;
END;

