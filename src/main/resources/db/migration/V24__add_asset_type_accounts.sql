-- Add optional account fields to asset_types
IF OBJECT_ID('asset_types', 'U') IS NOT NULL AND COL_LENGTH('asset_types', 'utility_account') IS NULL
BEGIN
    ALTER TABLE asset_types ADD utility_account NVARCHAR(128) NULL;
END;
GO
IF OBJECT_ID('asset_types', 'U') IS NOT NULL AND COL_LENGTH('asset_types', 'retirement_account') IS NULL
BEGIN
    ALTER TABLE asset_types ADD retirement_account NVARCHAR(128) NULL;
END;
GO

