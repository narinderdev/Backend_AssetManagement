-- Add nullable coordinates for asset locations.
-- Existing rows stay NULL by default.
IF OBJECT_ID('dbo.asset_location', 'U') IS NOT NULL AND COL_LENGTH('dbo.asset_location', 'latitude') IS NULL
BEGIN
    ALTER TABLE dbo.asset_location ADD latitude DECIMAL(10,7) NULL;
END;

IF OBJECT_ID('dbo.asset_location', 'U') IS NOT NULL AND COL_LENGTH('dbo.asset_location', 'longitude') IS NULL
BEGIN
    ALTER TABLE dbo.asset_location ADD longitude DECIMAL(10,7) NULL;
END;
