-- Remove user_password_meta table (no longer used)
IF OBJECT_ID('user_password_meta', 'U') IS NOT NULL
BEGIN
    DROP TABLE user_password_meta;
END
