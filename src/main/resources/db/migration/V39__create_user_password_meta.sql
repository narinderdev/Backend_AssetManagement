-- Track per-user password change date
IF OBJECT_ID('user_password_meta', 'U') IS NULL
BEGIN
    CREATE TABLE user_password_meta (
        user_id BIGINT NOT NULL,
        password_changed_at DATE NOT NULL,
        CONSTRAINT pk_user_password_meta PRIMARY KEY (user_id)
    );
END;

IF OBJECT_ID('users', 'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_user_password_meta_user')
    BEGIN
        ALTER TABLE user_password_meta
        ADD CONSTRAINT fk_user_password_meta_user
            FOREIGN KEY (user_id) REFERENCES users(id);
    END;
END;
