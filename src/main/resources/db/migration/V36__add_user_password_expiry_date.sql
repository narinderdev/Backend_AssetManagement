-- Add password expiry date for users
IF OBJECT_ID('users', 'U') IS NOT NULL AND COL_LENGTH('users', 'password_expiry_date') IS NULL
BEGIN
    ALTER TABLE users ADD password_expiry_date DATE NULL;
END

