-- Remove password expiry date from users table (moved to policy table)
IF OBJECT_ID('users', 'U') IS NOT NULL AND COL_LENGTH('users', 'password_expiry_date') IS NOT NULL
BEGIN
    ALTER TABLE users DROP COLUMN password_expiry_date;
END

