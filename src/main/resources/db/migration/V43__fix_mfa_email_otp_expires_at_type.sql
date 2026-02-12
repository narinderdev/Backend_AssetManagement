IF COL_LENGTH('users', 'mfa_email_otp_expires_at') IS NOT NULL
BEGIN
    ALTER TABLE users
        ALTER COLUMN mfa_email_otp_expires_at DATETIMEOFFSET(7) NULL;
END;
