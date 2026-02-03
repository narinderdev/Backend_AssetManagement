-- Move useful life years from warranty/lifecycle to financial details and drop the old column
IF COL_LENGTH('asset_financial_details', 'useful_life_years') IS NULL
BEGIN
    ALTER TABLE asset_financial_details ADD useful_life_years INT NULL;
END;
GO

-- Populate financial useful life when missing, using existing warranty data
UPDATE fin
SET fin.useful_life_years = COALESCE(fin.useful_life_years, wl.expected_useful_life_years)
FROM asset_financial_details fin
JOIN asset_warranty_lifecycle wl ON fin.asset_id = wl.asset_id
WHERE wl.expected_useful_life_years IS NOT NULL
  AND fin.useful_life_years IS NULL;
GO

-- Remove the deprecated column from warranty/lifecycle
IF COL_LENGTH('asset_warranty_lifecycle', 'expected_useful_life_years') IS NOT NULL
BEGIN
    ALTER TABLE asset_warranty_lifecycle DROP COLUMN expected_useful_life_years;
END;
GO
